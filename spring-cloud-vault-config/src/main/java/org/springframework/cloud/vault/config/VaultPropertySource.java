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

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link EnumerablePropertySource} backed by {@link VaultConfigTemplate}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@Slf4j
class VaultPropertySource extends EnumerablePropertySource<VaultConfigTemplate> {

	private final VaultProperties vaultProperties;
	private final SecureBackendAccessor secureBackendAccessor;
	private final Map<String, String> properties = new LinkedHashMap<>();
	private Secrets secrets;

	/**
	 * Creates a new {@link VaultPropertySource}.
	 *
	 * @param operations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param secureBackendAccessor must not be {@literal null}.
	 */
	public VaultPropertySource(VaultConfigTemplate operations, VaultProperties properties,
			SecureBackendAccessor secureBackendAccessor) {

		super(secureBackendAccessor.getName(), operations);

		Assert.notNull(operations, "VaultConfigTemplate must not be null!");
		Assert.notNull(properties, "VaultProperties must not be null!");
		Assert.notNull(secureBackendAccessor, "SecureBackendAccessor must not be null!");

		this.vaultProperties = properties;
		this.secureBackendAccessor = secureBackendAccessor;
	}

	/**
	 * Initialize property source and read properties from Vault.
	 */
	public void init() {

		try {
			this.secrets = this.source.read(this.secureBackendAccessor);
			if (this.secrets != null) {
				this.properties.putAll(secrets.getData());
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

	Secrets getSecrets() {
		return secrets;
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
