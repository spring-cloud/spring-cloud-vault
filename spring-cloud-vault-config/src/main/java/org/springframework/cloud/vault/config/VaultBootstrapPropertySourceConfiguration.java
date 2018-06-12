/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.vault.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.vault.config.VaultBootstrapConfiguration.TaskSchedulerWrapper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;

/**
 * {@link org.springframework.cloud.bootstrap.BootstrapConfiguration Auto-configuration}
 * for Spring Vault's {@link PropertySourceLocator} support.
 *
 * @author Mark Paluch
 * @since 1.1
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ VaultGenericBackendProperties.class,
		VaultKeyValueBackendProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class VaultBootstrapPropertySourceConfiguration implements InitializingBean {

	private final ConfigurableApplicationContext applicationContext;

	private Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors;

	private Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories;

	public VaultBootstrapPropertySourceConfiguration(
			ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {

		this.vaultSecretBackendDescriptors = applicationContext.getBeansOfType(
				VaultSecretBackendDescriptor.class).values();

		this.factories = (Collection) applicationContext.getBeansOfType(
				SecretBackendMetadataFactory.class).values();
	}

	@Bean
	public PropertySourceLocator vaultPropertySourceLocator(VaultOperations operations,
			VaultProperties vaultProperties,
			VaultKeyValueBackendProperties kvBackendProperties,
			VaultGenericBackendProperties genericBackendProperties,
			ObjectFactory<SecretLeaseContainer> secretLeaseContainerObjectFactory) {

		VaultConfigTemplate vaultConfigTemplate = new VaultConfigTemplate(operations,
				vaultProperties);

		PropertySourceLocatorConfiguration configuration = getPropertySourceConfiguration(Arrays
				.asList(kvBackendProperties, genericBackendProperties));

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {

			// This is to destroy bootstrap resources
			// otherwise, the bootstrap context is not shut down cleanly
			applicationContext.registerShutdownHook();

			SecretLeaseContainer secretLeaseContainer = secretLeaseContainerObjectFactory
					.getObject();
			secretLeaseContainer.start();

			return new LeasingVaultPropertySourceLocator(vaultProperties,
 configuration,
					secretLeaseContainer);
		}

		return new VaultPropertySourceLocator(vaultConfigTemplate, vaultProperties,
				configuration);
	}

	/**
	 * Apply configuration through {@link VaultConfigurer}.
	 *
	 * @param keyValueBackends configured backend (key-value, generic secret backend).
	 * @return
	 */
	private PropertySourceLocatorConfiguration getPropertySourceConfiguration(
			List<VaultKeyValueBackendPropertiesSupport> keyValueBackends) {

		Collection<VaultConfigurer> configurers = applicationContext.getBeansOfType(
				VaultConfigurer.class).values();

		DefaultSecretBackendConfigurer secretBackendConfigurer = new DefaultSecretBackendConfigurer();

		if (configurers.isEmpty()) {
			secretBackendConfigurer.registerDefaultGenericSecretBackends(true)
					.registerDefaultDiscoveredSecretBackends(true);
		}
		else {

			for (VaultConfigurer vaultConfigurer : configurers) {
				vaultConfigurer.addSecretBackends(secretBackendConfigurer);
			}
		}

		if (secretBackendConfigurer.isRegisterDefaultGenericSecretBackends()) {

			for (VaultKeyValueBackendPropertiesSupport keyValueBackend : keyValueBackends) {

				if (!keyValueBackend.isEnabled()) {
					continue;
				}

				List<String> contexts = KeyValueSecretBackendMetadata.buildContexts(
						keyValueBackend, Arrays.asList(applicationContext
								.getEnvironment().getActiveProfiles()));

				if (keyValueBackend instanceof VaultKeyValueBackendProperties
						&& ((VaultKeyValueBackendProperties) keyValueBackend)
								.getBackendVersion() == 2) {

					for (String context : contexts) {
						secretBackendConfigurer.add(KeyValueSecretBackendMetadata.create(
								keyValueBackend.getBackend(), context));
					}
				}
				else {
					for (String context : contexts) {
						secretBackendConfigurer.add(GenericSecretBackendMetadata.create(
								keyValueBackend.getBackend(), context));
					}
				}
			}

			Collection<SecretBackendMetadata> backendAccessors = SecretBackendFactories
					.createSecretBackendMetadata(vaultSecretBackendDescriptors, factories);

			backendAccessors.forEach(secretBackendConfigurer::add);
		}

		if (secretBackendConfigurer.isRegisterDefaultDiscoveredSecretBackends()) {

			Collection<SecretBackendMetadata> backendAccessors = SecretBackendFactories
					.createSecretBackendMetadata(vaultSecretBackendDescriptors, factories);

			backendAccessors.forEach(secretBackendConfigurer::add);
		}

		return secretBackendConfigurer;
	}

	/**
	 * @return the {@link SessionManager} for Vault session management.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean
	public SecretLeaseContainer secretLeaseContainer(VaultOperations vaultOperations,
			TaskSchedulerWrapper taskSchedulerWrapper) {
		return new SecretLeaseContainer(vaultOperations,
				taskSchedulerWrapper.getTaskScheduler());
	}
}
