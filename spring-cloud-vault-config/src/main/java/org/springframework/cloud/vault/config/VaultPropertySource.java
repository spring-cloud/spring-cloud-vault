/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link EnumerablePropertySource} backed by {@link VaultConfigTemplate}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
class VaultPropertySource extends EnumerablePropertySource<VaultConfigOperations> {

	private static final Log log = LogFactory.getLog(VaultPropertySource.class);

	private final boolean failFast;

	private final SecretBackendMetadata secretBackendMetadata;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	@Nullable
	private Secrets secrets;

	/**
	 * Creates a new {@link VaultPropertySource}.
	 * @param operations must not be {@literal null}.
	 * @param failFast fail if properties could not be read because of access errors.
	 * @param secretBackendMetadata must not be {@literal null}.
	 */
	VaultPropertySource(VaultConfigOperations operations, boolean failFast,
			SecretBackendMetadata secretBackendMetadata) {

		super(secretBackendMetadata.getName(), operations);

		Assert.notNull(operations, "VaultConfigTemplate must not be null!");
		Assert.notNull(secretBackendMetadata, "SecretBackendMetadata must not be null!");

		this.failFast = failFast;
		this.secretBackendMetadata = secretBackendMetadata;
	}

	/**
	 * Initialize property source and read properties from Vault.
	 */
	public void init() {

		try {
			this.secrets = this.source.read(this.secretBackendMetadata);
			if (this.secrets != null) {
				this.properties.putAll(this.secrets.getRequiredData());
			}
		}
		catch (RuntimeException e) {

			String message = String.format("Unable to read properties from Vault using %s for %s ", getName(),
					this.secretBackendMetadata.getVariables());

			if (this.failFast) {
				throw e;
			}

			log.error(message, e);
		}
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.keySet();
		return strings.toArray(new String[0]);
	}

}
