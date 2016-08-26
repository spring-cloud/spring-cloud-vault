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
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultClientResponse;
import org.springframework.cloud.vault.VaultHealthResponse;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.util.Assert;

/**
 * This class encapsulates main Vault interaction. {@link VaultTemplate} will log into
 * Vault on initialization and use the token throughout the whole lifetime.
 *
 * @author Mark Paluch
 */
public class VaultTemplate implements InitializingBean, VaultOperations {

	private final VaultProperties properties;
	private final VaultClient client;
	private final ClientAuthentication clientAuthentication;
	private final transient VaultState vaultState = new VaultState();
	private final VaultSession vaultSession;

	/**
	 * Creates a new {@link VaultConfigTemplate} for the given {@link VaultProperties},
	 * {@link VaultClient} and {@link ClientAuthentication}.
	 *
	 * @param properties must not be {@literal null}.
	 * @param client must not be {@literal null}.
	 * @param clientAuthentication must not be {@literal null}.
	 */
	public VaultTemplate(VaultProperties properties, VaultClient client,
			ClientAuthentication clientAuthentication) {

		Assert.notNull(properties, "VaultProperties must not be null!");
		Assert.notNull(client, "VaultClient must not be null!");
		Assert.notNull(clientAuthentication, "ClientAuthentication must not be null!");

		this.properties = properties;
		this.client = client;
		this.clientAuthentication = clientAuthentication;
		this.vaultSession = new VaultSession() {
			@Override
			public VaultClientResponse read(URI uri) {
				return VaultTemplate.this.client.read(uri, getToken());
			}

			@Override
			public VaultClientResponse write(URI uri, Object entity) {
				return VaultTemplate.this.client.write(uri, entity, getToken());
			}
		};
	}

	@Override
	public void afterPropertiesSet() {
		login();
	}

	private void login() {
		vaultState.setToken(clientAuthentication.login());
	}

	private VaultToken getToken() {

		if (vaultState.getToken() == null) {
			login();
		}

		return vaultState.getToken();
	}

	@Override
	public VaultConfigOperations opsForConfig() {
		return new VaultConfigTemplate(this, properties);
	}

	@Override
	public <T> T doWithVault(String path, SessionCallback sessionCallback) {

		Assert.notNull(sessionCallback, "SessionCallback must not be null!");

		URI uri = client.buildUri(properties, path);
		return sessionCallback.doWithVault(uri, vaultSession);
	}

	@Override
	public <T> T doWithVault(String pathTemplate, Map<String, ?> variables,
			SessionCallback sessionCallback) {

		Assert.notNull(sessionCallback, "SessionCallback must not be null!");

		URI uri = client.buildUri(properties, pathTemplate, variables);
		return sessionCallback.doWithVault(uri, vaultSession);
	}

	private final static String HEALTH_URL_TEMPLATE = "sys/health";

	/**
	 * Query the current Vault service for it's health status
	 *
	 * @return A {@link VaultHealthResponse} containing the current service status.
	 */
	public VaultHealthResponse health() {
		URI uri = client.buildUri(properties, HEALTH_URL_TEMPLATE);
		return client.health(uri);
	}

	/**
	 * Check whether Vault is available (vault created and unsealed).
	 *
	 * @return
	 */
	public boolean isAvailable() {
		try{
			VaultHealthResponse health = health();
			return health.isInitialized() && !health.isSealed();
		} catch(Exception e) {
			return false;
		}
	}
}
