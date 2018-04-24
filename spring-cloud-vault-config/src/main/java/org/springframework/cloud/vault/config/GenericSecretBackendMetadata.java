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

import java.util.List;

import org.springframework.util.Assert;

/**
 * {@link SecretBackendMetadata} for the {@code generic} secret backend.
 *
 * @author Mark Paluch
 */
public class GenericSecretBackendMetadata extends KeyValueSecretBackendMetadata implements
		SecretBackendMetadata {

	private GenericSecretBackendMetadata(String path) {
		super(path);
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code generic} secret backend given
	 * a {@code secretBackendPath} and {@code key}.
	 *
	 * @param secretBackendPath the secret backend mount path without leading/trailing
	 * slashes, must not be empty or {@literal null}.
	 * @param key the key within the secret backend. May contain slashes but not
	 * leading/trailing slashes, must not be empty or {@literal null}.
	 * @return the {@link SecretBackendMetadata}
	 */
	public static SecretBackendMetadata create(String secretBackendPath, String key) {

		Assert.hasText(secretBackendPath, "Secret backend path must not be null or empty");
		Assert.hasText(key, "Key must not be null or empty");

		return create(String.format("%s/%s", secretBackendPath, key));
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code generic} secret backend given
	 * a {@code path}.
	 *
	 * @param path the relative path of the secret. slashes, must not be empty or
	 * {@literal null}.
	 * @return the {@link SecretBackendMetadata}
	 * @since 1.1
	 */
	public static SecretBackendMetadata create(String path) {
		return new GenericSecretBackendMetadata(path);
	}

	/**
	 * Build a list of context paths from application name and the active profile names.
	 * Application name and profiles support multiple (comma-separated) values.
	 *
	 * @param properties
	 * @param profiles active application profiles.
	 * @return list of context paths.
	 */
	public static List<String> buildContexts(VaultGenericBackendProperties properties,
			List<String> profiles) {
		return KeyValueSecretBackendMetadata.buildContexts(properties, profiles);
	}

	/**
	 * Create a list of context names from a combination of application name and
	 * application name with profile name. Using an empty application name will return an
	 * empty list.
	 *
	 * @param applicationName the application name. May be empty.
	 * @param profiles active application profiles.
	 * @param profileSeparator profile separator character between application name and
	 * profile name.
	 * @return list of context names.
	 * @since 1.1
	 */
	public static List<String> buildContexts(String applicationName,
			List<String> profiles, String profileSeparator) {
		return KeyValueSecretBackendMetadata.buildContexts(applicationName, profiles,
				profileSeparator);
	}
}
