/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.List;

/**
 * Factory for {@link PropertySourceLocatorConfigurationFactory}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class PropertySourceLocatorConfigurationFactory {

	private final Collection<VaultConfigurer> configurers;

	private final Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors;

	private final Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories;

	PropertySourceLocatorConfigurationFactory(Collection<VaultConfigurer> configurers,
			Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors,
			Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories) {
		this.configurers = configurers;
		this.vaultSecretBackendDescriptors = vaultSecretBackendDescriptors;
		this.factories = factories;
	}

	/**
	 * Apply configuration through {@link VaultConfigurer}.
	 * @param keyValueBackends configured backend.
	 * @return the {@link PropertySourceLocatorConfiguration}.
	 */
	PropertySourceLocatorConfiguration getPropertySourceConfiguration(
			List<VaultKeyValueBackendPropertiesSupport> keyValueBackends) {

		DefaultSecretBackendConfigurer secretBackendConfigurer = new DefaultSecretBackendConfigurer();

		if (this.configurers.isEmpty()) {
			secretBackendConfigurer.registerDefaultKeyValueSecretBackends(true)
					.registerDefaultDiscoveredSecretBackends(true);
		}
		else {

			for (VaultConfigurer vaultConfigurer : this.configurers) {
				vaultConfigurer.addSecretBackends(secretBackendConfigurer);
			}
		}

		if (secretBackendConfigurer.isRegisterDefaultKeyValueSecretBackends()) {

			for (VaultKeyValueBackendPropertiesSupport keyValueBackend : keyValueBackends) {

				if (!keyValueBackend.isEnabled()) {
					continue;
				}

				List<String> contexts = KeyValueSecretBackendMetadata.buildContexts(keyValueBackend,
						keyValueBackend.getProfiles());

				for (String context : contexts) {
					secretBackendConfigurer
							.add(KeyValueSecretBackendMetadata.create(keyValueBackend.getBackend(), context));
				}
			}

			Collection<SecretBackendMetadata> backendAccessors = SecretBackendFactories
					.createSecretBackendMetadata(this.vaultSecretBackendDescriptors, this.factories);

			backendAccessors.forEach(secretBackendConfigurer::add);
		}

		if (secretBackendConfigurer.isRegisterDefaultDiscoveredSecretBackends()) {

			Collection<SecretBackendMetadata> backendAccessors = SecretBackendFactories
					.createSecretBackendMetadata(this.vaultSecretBackendDescriptors, this.factories);

			backendAccessors.forEach(secretBackendConfigurer::add);
		}

		return secretBackendConfigurer;
	}

}
