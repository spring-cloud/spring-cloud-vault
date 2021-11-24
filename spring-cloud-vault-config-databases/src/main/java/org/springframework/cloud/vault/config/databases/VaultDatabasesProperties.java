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

package org.springframework.cloud.vault.config.databases;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptorFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for multiple database secrets using the {@code database}
 * backend. This is configured with the {@code spring.cloud.vault.databases.*} mapping.
 *
 * @author Quintin Beukes
 * @author Mark Paluch
 * @since 3.0.3
 */
@ConfigurationProperties("spring.cloud.vault")
public class VaultDatabasesProperties implements VaultSecretBackendDescriptorFactory {

	private Map<String, VaultDatabaseProperties> databases = new HashMap<>();

	public Map<String, VaultDatabaseProperties> getDatabases() {
		return this.databases;
	}

	public void setDatabases(Map<String, VaultDatabaseProperties> databases) {
		this.databases = databases;
	}

	@Override
	public Collection<? extends VaultSecretBackendDescriptor> create() {
		return getDatabases().values();
	}

}
