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
package org.springframework.cloud.vault;

import org.springframework.web.client.RestTemplate;

/**
 * @author Mark Paluch
 */
public abstract class ClientAuthentication {

	/**
	 * Perform a login to Vault and return a {@link VaultToken}.
	 *
	 * @return a {@link VaultToken}.
	 */
	public abstract VaultToken login();

	/**
	 * Creates a Token-based authentication adapter.
	 * 
	 * @param vaultProperties must not be {@literal null}.
	 * @return the {@link ClientAuthentication} adapter.
	 */
	public static ClientAuthentication token(VaultProperties vaultProperties) {
		return new TokenClientAuthentication(vaultProperties);
	}

	/**
	 * Creates a generic authentication adapter.
	 * 
	 * @param vaultProperties must not be {@literal null}.
	 * @param vaultClient must not be {@literal null}.
	 * @return the {@link ClientAuthentication} adapter.
	 */
	public static ClientAuthentication create(VaultProperties vaultProperties,
			VaultClient vaultClient) {
		return new DefaultClientAuthentication(vaultProperties, vaultClient);
	}

	/**
	 * Creates an AppId-based authentication adapter.
	 *
	 * @param vaultProperties must not be {@literal null}.
	 * @return the {@link ClientAuthentication} adapter.
	 */
	public static ClientAuthentication appId(VaultProperties vaultProperties,
			VaultClient vaultClient) {
		return new DefaultClientAuthentication(vaultProperties, vaultClient);
	}

	/**
	 * Creates an AppId-based authentication adapter.
	 * 
	 * @param vaultProperties must not be {@literal null}.
	 * @param vaultClient must not be {@literal null}.
	 * @param userIdMechanism must not be {@literal null}.
	 * @return the {@link ClientAuthentication} adapter.
	 */
	public static ClientAuthentication appId(VaultProperties vaultProperties,
			VaultClient vaultClient, AppIdUserIdMechanism userIdMechanism) {
		return new DefaultClientAuthentication(vaultProperties, vaultClient,
				userIdMechanism);
	}
}
