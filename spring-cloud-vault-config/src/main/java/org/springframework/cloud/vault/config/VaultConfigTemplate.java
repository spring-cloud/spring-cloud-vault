/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.vault.client.VaultResponseEntity;
import org.springframework.vault.core.VaultOperations;

import lombok.extern.slf4j.Slf4j;

/**
 * Central class to retrieve configuration from Vault.
 * 
 * @author Mark Paluch
 * @see VaultOperations
 */
@Slf4j
public class VaultConfigTemplate implements VaultConfigOperations {

	private final VaultOperations vaultOperations;
	private final VaultProperties properties;

	/**
	 * Create a new {@link VaultConfigTemplate} given {@link VaultOperations}.
	 *
	 * @param vaultOperations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 */
	public VaultConfigTemplate(VaultOperations vaultOperations,
			VaultProperties properties) {

		Assert.notNull(vaultOperations, "VaultOperations must not be null!");
		Assert.notNull(properties, "VaultProperties must not be null!");

		this.vaultOperations = vaultOperations;
		this.properties = properties;
	}

	@Override
	public Secrets read(final SecretBackendMetadata secretBackendMetadata) {

		Assert.notNull(secretBackendMetadata, "SecureBackendAccessor must not be null!");

		VaultResponseEntity<Secrets> response = vaultOperations.doWithVault(
				new VaultOperations.SessionCallback<VaultResponseEntity<Secrets>>() {
					@Override
					public VaultResponseEntity<Secrets> doWithVault(
							VaultOperations.VaultSession session) {

						return session.exchange("{backend}/{key}", HttpMethod.GET, null,
								Secrets.class, secretBackendMetadata.getVariables());
					}
				});

		log.info(String.format("Fetching config from Vault at: %s", response.getUri()));

		if (response.getStatusCode() == HttpStatus.OK) {

			Secrets secrets = response.getBody();

			PropertyTransformer propertyTransformer = secretBackendMetadata
					.getPropertyTransformer();

			if (propertyTransformer != null) {
				secrets.setData(
						propertyTransformer.transformProperties(secrets.getData()));
			}

			return secrets;
		}

		if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
			log.info(String.format("Could not locate PropertySource: %s",
					"key not found"));
		}
		else if (properties.isFailFast()) {
			throw new IllegalStateException(String.format(
					"Could not locate PropertySource and the fail fast property is set, failing Status %d %s",
					response.getStatusCode().value(), response.getMessage()));
		}
		else {
			log.warn(String.format("Could not locate PropertySource: Status %d %s",
					response.getStatusCode().value(), response.getMessage()));
		}

		return null;
	}

	public VaultOperations getVaultOperations() {
		return vaultOperations;
	}
}
