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

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultClientResponse;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.config.VaultOperations.SessionCallback;
import org.springframework.cloud.vault.config.VaultOperations.VaultSession;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import lombok.extern.apachecommons.CommonsLog;

import org.apache.commons.logging.Log;

/**
 * Central class to retrieve configuration from Vault.
 * 
 * @author Mark Paluch
 * @see VaultClient
 * @see ClientAuthentication
 */
@CommonsLog
public class VaultConfigTemplate implements VaultConfigOperations {

	private final VaultOperations vaultOperations;
	private final VaultProperties properties;
	private final VaultConfigSessionCallback callback;

	/**
	 * Creates a new {@link VaultConfigTemplate}.
	 *
	 * @param vaultOperations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 */
	public VaultConfigTemplate(VaultOperations vaultOperations, VaultProperties properties) {

		Assert.notNull(vaultOperations, "VaultOperations must not be null!");
		Assert.notNull(properties, "VaultProperties must not be null!");

		this.vaultOperations = vaultOperations;
		this.properties = properties;
		this.callback = new VaultConfigSessionCallback(log);
	}

	@Override
	public Map<String, String> read(SecureBackendAccessor secureBackendAccessor) {

		Assert.notNull(secureBackendAccessor, "SecureBackendAccessor must not be null!");

		VaultClientResponse response = vaultOperations.doWithVault("{backend}/{key}",
				secureBackendAccessor.variables(), callback);

		if (response.getStatusCode() == HttpStatus.OK) {
			return secureBackendAccessor
					.transformProperties(response.getBody().getData());
		}

		if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
			log.info(String
					.format("Could not locate PropertySource: %s", "key not found"));
		}
		else if (properties.isFailFast()) {
			throw new IllegalStateException(
					String.format(
							"Could not locate PropertySource and the fail fast property is set, failing Status %d %s",
							response.getStatusCode().value(), response.getMessage()));
		}
		else {
			log.warn(String.format("Could not locate PropertySource: Status %d %s",
					response.getStatusCode().value(), response.getMessage()));
		}

		return Collections.emptyMap();
	}

	static class VaultConfigSessionCallback implements SessionCallback {

		private final Log log;

		public VaultConfigSessionCallback(Log log) {
			this.log = log;
		}

		@Override
		public VaultClientResponse doWithVault(URI uri, VaultSession session) {
			log.info(String.format("Fetching config from Vault at: %s", uri));
			return session.read(uri);
		}
	}

}
