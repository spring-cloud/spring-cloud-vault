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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultProperties.AppIdProperties;
import org.springframework.cloud.vault.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.Assert;

import lombok.extern.apachecommons.CommonsLog;

/**
 * A {@link EnumerablePropertySource} backed by {@link VaultClient}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@CommonsLog
class VaultPropertySource extends EnumerablePropertySource<VaultClient> {

	private final VaultProperties vaultProperties;
	private final SecureBackendAccessor secureBackendAccessor;
	private final Map<String, String> properties = new LinkedHashMap<>();
	private final ClientAuthentication clientAuthentication;
	private final transient VaultState vaultState;

	/**
	 * Creates a new {@link VaultPropertySource}.
	 *
	 * @param vaultClient must not be {@literal null}.
	 * @param clientAuthentication mist not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param state shared Vault state, must not be {@literal null}.
	 * @param secureBackendAccessor must not be {@literal null}.
	 */
	public VaultPropertySource(VaultClient vaultClient,
			ClientAuthentication clientAuthentication, VaultProperties properties,
			VaultState state, SecureBackendAccessor secureBackendAccessor) {

		super(secureBackendAccessor.getName(), vaultClient);

		Assert.notNull(vaultClient, "VaultClient must not be null!");
		Assert.notNull(properties, "VaultProperties must not be null!");
		Assert.notNull(state, "VaultState must not be null!");
		Assert.notNull(secureBackendAccessor, "SecureBackendAccessor must not be null!");
		Assert.notNull(clientAuthentication, "ClientAuthentication must not be null!");

		this.vaultProperties = properties;
		this.clientAuthentication = clientAuthentication;
		this.vaultState = state;
		this.secureBackendAccessor = secureBackendAccessor;
	}

	/**
	 * Initialize property source and read properties from Vault.
	 */
	public void init() {

		try {
			Map<String, String> values = this.source.read(this.secureBackendAccessor,
					obtainToken());
			if (values != null) {
				this.properties.putAll(values);
			}
		}
		catch (Exception e) {

			String message = String.format(
					"Unable to read properties from Vault using %s for %s ", getName(),
					secureBackendAccessor.variables());
			if (vaultProperties.isFailFast()) {
				if (e instanceof RuntimeException) {
					throw e;
				}

				throw new IllegalStateException(message, e);
			}

			log.error(message, e);
		}
	}

	private VaultToken obtainToken() {

		if (vaultState.getToken() != null) {
			return vaultState.getToken();
		}

		if (vaultProperties.getAuthentication() == AuthenticationMethod.TOKEN) {

			Assert.hasText(vaultProperties.getToken(), "Vault Token must not be empty");
			vaultState.setToken(clientAuthentication.login());

			return vaultState.getToken();
		}

		if (vaultProperties.getAuthentication() == AuthenticationMethod.AWS_EC2) {

			vaultState.setToken(clientAuthentication.login());
			return vaultState.getToken();
		}

		if (vaultProperties.getAuthentication() == AuthenticationMethod.APPID) {

			AppIdProperties appIdProperties = vaultProperties.getAppId();
			Assert.hasText(vaultProperties.getApplicationName(),
					"AppId must not be empty");
			Assert.hasText(appIdProperties.getAppIdPath(), "AppIdPath must not be empty");

			vaultState.setToken(clientAuthentication.login());
			return vaultState.getToken();
		}

		throw new IllegalStateException(
				String.format("Authentication method %s not supported",
						vaultProperties.getAuthentication()));
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}
}