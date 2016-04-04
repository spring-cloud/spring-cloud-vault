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

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of common used {@link SecureBackendAccessor accessors} to access secure
 * backends.
 * @author Mark Paluch
 */
public class SecureBackendAccessors {

	/**
	 * Creates a {@link SecureBackendAccessor} for the {@code generic} secure backend.
	 *
	 * @param vaultProperties
	 * @param key
	 * @return
	 */
	public static SecureBackendAccessor generic(VaultProperties vaultProperties,
			String key) {
		return generic(vaultProperties.getBackend(), key);
	}

	/**
	 * Creates a {@link SecureBackendAccessor} for the {@code generic} secure backend.
	 *
	 * @param secretBackendPath
	 * @param key
	 * @return
	 */
	public static SecureBackendAccessor generic(final String secretBackendPath,
			final String key) {
		return new SecureBackendAccessor() {

			@Override
			public Map<String, String> variables() {
				Map<String, String> variables = new HashMap<>();
				variables.put("backend", secretBackendPath);
				variables.put("key", key);
				return variables;
			}

			@Override
			public Map<String, String> transformProperties(Map<String, String> input) {
				return input;
			}
		};
	}

	/**
	 * Creates a {@link SecureBackendAccessor} for a secure backend using {@link org.springframework.cloud.vault.VaultProperties.DatabaseSecretProperties}. This
	 * accessor transforms Vault's username/password property names to names provided with
	 * {@link VaultProperties.DatabaseSecretProperties#getUsernameProperty()} and
	 * {@link VaultProperties.DatabaseSecretProperties#getUsernameProperty()}.
	 * 
	 * @param properties
	 * @return
	 */
	public static SecureBackendAccessor database(final VaultProperties.DatabaseSecretProperties properties) {
		return new SecureBackendAccessor() {

			@Override
			public Map<String, String> variables() {
				Map<String, String> variables = new HashMap<>();
				variables.put("backend", properties.getBackend());
				variables.put("key", String.format("creds/%s", properties.getRole()));
				return variables;
			}

			@Override
			public Map<String, String> transformProperties(Map<String, String> input) {

				Map<String, String> result = new HashMap();
				result.put(properties.getUsernameProperty(), input.get("username"));
				result.put(properties.getPasswordProperty(), input.get("password"));

				return result;
			}
		};
	}

}
