/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

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
 */
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VaultKeyValueBackendProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class VaultBootstrapPropertySourceConfiguration implements InitializingBean {

	private final ConfigurableApplicationContext applicationContext;

	private Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors;

	private Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories;

	public VaultBootstrapPropertySourceConfiguration(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {

		this.vaultSecretBackendDescriptors = this.applicationContext.getBeansOfType(VaultSecretBackendDescriptor.class)
				.values();

		this.factories = (Collection) this.applicationContext.getBeansOfType(SecretBackendMetadataFactory.class)
				.values();
	}

	@Bean
	public PropertySourceLocator vaultPropertySourceLocator(VaultOperations operations, VaultProperties vaultProperties,
			VaultKeyValueBackendProperties kvBackendProperties,
			ObjectFactory<SecretLeaseContainer> secretLeaseContainerObjectFactory) {

		VaultConfigTemplate vaultConfigTemplate = new VaultConfigTemplate(operations, vaultProperties);

		Collection<VaultConfigurer> vaultConfigurers = this.applicationContext.getBeansOfType(VaultConfigurer.class)
				.values();
		PropertySourceLocatorConfigurationFactory factory = new PropertySourceLocatorConfigurationFactory(
				vaultConfigurers, this.vaultSecretBackendDescriptors, this.factories);

		PropertySourceLocatorConfiguration configuration = factory
				.getPropertySourceConfiguration(Collections.singletonList(kvBackendProperties));

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
	 * @param vaultProperties the {@link VaultProperties}.
	 * @param vaultOperations the {@link VaultOperations}.
	 * @param taskSchedulerWrapper the {@link TaskSchedulerWrapper}.
	 * @return the {@link SessionManager} for Vault session management.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public SecretLeaseContainer secretLeaseContainer(VaultProperties vaultProperties, VaultOperations vaultOperations,
			TaskSchedulerWrapper taskSchedulerWrapper) {

		VaultProperties.ConfigLifecycle lifecycle = vaultProperties.getConfig().getLifecycle();

		SecretLeaseContainer container = new SecretLeaseContainer(vaultOperations,
				taskSchedulerWrapper.getTaskScheduler());

		customizeContainer(lifecycle, container);

		return container;
	}

	static void customizeContainer(VaultProperties.ConfigLifecycle lifecycle, SecretLeaseContainer container) {

		if (lifecycle.isEnabled()) {

			if (lifecycle.getMinRenewal() != null) {
				container.setMinRenewal(lifecycle.getMinRenewal());
			}

			if (lifecycle.getExpiryThreshold() != null) {
				container.setExpiryThreshold(lifecycle.getExpiryThreshold());
			}

			if (lifecycle.getLeaseEndpoints() != null) {
				container.setLeaseEndpoints(lifecycle.getLeaseEndpoints());
			}
		}
	}

}
