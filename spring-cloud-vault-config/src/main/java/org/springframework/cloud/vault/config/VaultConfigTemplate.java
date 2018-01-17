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

import java.util.Map;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.util.Assert;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.support.JsonMapFlattener;
import org.springframework.vault.support.VaultResponse;

/**
 * Central class to retrieve configuration from Vault.
 *
 * @author Mark Paluch
 * @see VaultOperations
 */
@CommonsLog
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
	public Secrets read(SecretBackendMetadata secretBackendMetadata) {

		Assert.notNull(secretBackendMetadata, "SecureBackendAccessor must not be null!");

		log.info(String.format("Fetching config from Vault at: %s",
				secretBackendMetadata.getPath()));

		try {
			VaultResponse vaultResponse = vaultOperations
					.read(secretBackendMetadata.getPath());

			if (vaultResponse == null) {

				log.info(String.format("Could not locate PropertySource: %s",
						"key not found"));
				return null;
			}

			Map<String, Object> data = JsonMapFlattener.flatten(vaultResponse.getData());
			PropertyTransformer propertyTransformer = secretBackendMetadata
					.getPropertyTransformer();

			if (propertyTransformer != null) {
				data = propertyTransformer.transformProperties(data);
			}

			return createSecrets(vaultResponse, data);
		}
		catch (VaultException e) {

			if (properties.isFailFast()) {
				throw new IllegalStateException(
						"Could not locate PropertySource and the fail fast property is set, failing.",
						e);
			}
			else {
				log.warn(String.format("Could not locate PropertySource: %s",
						e.getMessage()));
			}
		}

		return null;
	}

	private Secrets createSecrets(VaultResponse vaultResponse, Map<String, Object> data) {

		Secrets secrets = new Secrets();

		secrets.setData(data);

		secrets.setAuth(vaultResponse.getAuth());
		secrets.setLeaseDuration(vaultResponse.getLeaseDuration());
		secrets.setMetadata(vaultResponse.getMetadata());
		secrets.setLeaseId(vaultResponse.getLeaseId());
		secrets.setRenewable(vaultResponse.isRenewable());
		secrets.setRequestId(vaultResponse.getRequestId());
		secrets.setWarnings(vaultResponse.getWarnings());
		secrets.setWrapInfo(vaultResponse.getWrapInfo());

		return secrets;
	}

	public VaultOperations getVaultOperations() {
		return vaultOperations;
	}
}
