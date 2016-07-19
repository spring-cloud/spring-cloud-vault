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

import org.springframework.cloud.vault.VaultProperties;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.Assert;

import lombok.extern.apachecommons.CommonsLog;

/**
 * A {@link EnumerablePropertySource} backed by {@link VaultConfigOperations}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@CommonsLog
class VaultPropertySource extends EnumerablePropertySource<VaultConfigOperations> {

	private final VaultProperties vaultProperties;
	private final SecureBackendAccessor secureBackendAccessor;
	private final Map<String, String> properties = new LinkedHashMap<>();

	/**
	 * Creates a new {@link VaultPropertySource}.
	 *
	 * @param operations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param secureBackendAccessor must not be {@literal null}.
	 */
	public VaultPropertySource(VaultConfigOperations operations,
			VaultProperties properties, SecureBackendAccessor secureBackendAccessor) {

		super(secureBackendAccessor.getName(), operations);

		Assert.notNull(operations, "VaultConfigOperations must not be null!");
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
			Map<String, String> values = this.source.read(this.secureBackendAccessor);
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
