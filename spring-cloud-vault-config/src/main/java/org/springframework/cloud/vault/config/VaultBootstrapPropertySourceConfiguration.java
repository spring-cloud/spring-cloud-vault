/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.vault.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;

import static org.springframework.cloud.vault.config.VaultAutoConfiguration.TaskSchedulerWrapper;

/**
 * {@link org.springframework.cloud.bootstrap.BootstrapConfiguration Auto-configuration}
 * for Spring Vault's {@link PropertySourceLocator} support.
 *
 * @author Mark Paluch
 * @author Grenville Wilson
 * @author Mårten Svantesson
 * @since 1.1
 * @deprecated since 3.0, use {@code spring.config.import=vault://} instead.
 */
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VaultKeyValueBackendProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Deprecated
public class VaultBootstrapPropertySourceConfiguration implements InitializingBean {

	private final VaultConfiguration configuration;

	private final ConfigurableApplicationContext applicationContext;

	@Nullable
	private Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors;

	@Nullable
	private Collection<VaultSecretBackendDescriptorFactory> vaultSecretBackendDescriptorFactories;

	@Nullable
	private Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories;

	public VaultBootstrapPropertySourceConfiguration(VaultProperties vaultProperties,
			ConfigurableApplicationContext applicationContext) {
		this.configuration = new VaultConfiguration(vaultProperties);
		this.applicationContext = applicationContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {

		this.vaultSecretBackendDescriptors = this.applicationContext.getBeansOfType(VaultSecretBackendDescriptor.class)
				.values();

		this.vaultSecretBackendDescriptorFactories = this.applicationContext
				.getBeansOfType(VaultSecretBackendDescriptorFactory.class).values();

		this.factories = (Collection) this.applicationContext.getBeansOfType(SecretBackendMetadataFactory.class)
				.values();
	}

	@Bean
	public PropertySourceLocator vaultPropertySourceLocator(VaultOperations operations, VaultProperties vaultProperties,
			VaultKeyValueBackendProperties kvBackendProperties,
			ObjectFactory<SecretLeaseContainer> secretLeaseContainerObjectFactory) {

		Assert.state(this.vaultSecretBackendDescriptors != null, "VaultSecretBackendDescriptors must not be null");
		Assert.state(this.factories != null, "SecretBackendMetadataFactories must not be null");

		VaultConfigTemplate vaultConfigTemplate = new VaultConfigTemplate(operations, vaultProperties);

		Collection<VaultConfigurer> vaultConfigurers = this.applicationContext.getBeansOfType(VaultConfigurer.class)
				.values();

		List<VaultSecretBackendDescriptor> descriptors = new ArrayList<>(this.vaultSecretBackendDescriptors);
		this.vaultSecretBackendDescriptorFactories.forEach(it -> descriptors.addAll(it.create()));

		PropertySourceLocatorConfigurationFactory factory = new PropertySourceLocatorConfigurationFactory(
				vaultConfigurers, descriptors, this.factories);

		PropertySourceLocatorConfiguration configuration = factory.getPropertySourceConfiguration(kvBackendProperties);

		VaultProperties.ConfigLifecycle lifecycle = vaultProperties.getConfig().getLifecycle();

		if (lifecycle.isEnabled()) {

			// This is to destroy bootstrap resources
			// otherwise, the bootstrap context is not shut down cleanly
			this.applicationContext.registerShutdownHook();

			SecretLeaseContainer secretLeaseContainer = secretLeaseContainerObjectFactory.getObject();

			secretLeaseContainer.start();

			return new LeasingVaultPropertySourceLocator(vaultProperties, configuration, secretLeaseContainer);
		}

		return new VaultPropertySourceLocator(vaultConfigTemplate, vaultProperties, configuration);
	}

	/**
	 * @param vaultOperations the {@link VaultOperations}.
	 * @param taskSchedulerWrapper the {@link TaskSchedulerWrapper}.
	 * @return the {@link SessionManager} for Vault session management.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public SecretLeaseContainer secretLeaseContainer(VaultOperations vaultOperations,
			TaskSchedulerWrapper taskSchedulerWrapper) {
		return this.configuration.createSecretLeaseContainer(vaultOperations, taskSchedulerWrapper::getTaskScheduler);
	}

}
