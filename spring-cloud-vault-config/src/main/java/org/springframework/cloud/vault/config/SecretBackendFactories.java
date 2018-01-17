/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.experimental.UtilityClass;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Utility class to create {@link SecretBackendMetadata} from a
 * {@link SecretBackendMetadataFactory}.
 *
 * @author Mark Paluch
 */
@CommonsLog
@UtilityClass
class SecretBackendFactories {

	public static Collection<SecretBackendMetadata> createSecretBackendMetadata(
			Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors,
			Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories) {

		List<SecretBackendMetadata> accessors = new ArrayList<>();

		for (VaultSecretBackendDescriptor vaultSecretBackendDescriptor : vaultSecretBackendDescriptors) {

			if (!vaultSecretBackendDescriptor.isEnabled()) {
				continue;
			}

			SecretBackendMetadata metadata = createSecretBackendMetadata(factories,
					vaultSecretBackendDescriptor);

			if (metadata == null) {
				log.warn(String.format("Cannot create SecretBackendMetadata for %s",
						vaultSecretBackendDescriptor));
				continue;
			}

			accessors.add(metadata);
		}

		return accessors;
	}

	private static SecretBackendMetadata createSecretBackendMetadata(
			Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories,
			VaultSecretBackendDescriptor vaultSecretBackendDescriptor) {

		SecretBackendMetadata accessor = null;
		for (SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor> factory : factories) {

			if (factory.supports(vaultSecretBackendDescriptor)) {
				accessor = factory.createMetadata(vaultSecretBackendDescriptor);
				break;
			}
		}
		return accessor;
	}
}
