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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.cloud.vault.config.GenericSecretBackendMetadata.*;

/**
 * Integration tests for {@link VaultConfigTemplate} using the generic secret backend.
 *
 * @author Mark Paluch
 */
public class GenericSecretIntegrationTests extends IntegrationTestSupport {

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;

	@Before
	public void setUp() {

		vaultProperties.setFailFast(false);
		prepare().getVaultOperations().write("secret/app-name", createData());

		configOperations = new VaultConfigTemplate(prepare().getVaultOperations(),
				vaultProperties);
	}

	@Test
	public void shouldReturnSecretsCorrectly() {

		Map<String, Object> secretProperties = configOperations
				.read(create("secret", "app-name")).getData();

		assertThat(secretProperties).containsAllEntriesOf(createExpectedMap());
	}

	@Test
	public void shouldReturnNullIfNotFound() {

		Secrets secrets = configOperations.read(create("secret", "missing"));

		assertThat(secrets).isNull();
	}

	private Map<String, Object> createData() {

		Map<String, Object> data = new HashMap<>();

		data.put("string", "value");
		data.put("number", 1234);
		data.put("boolean", true);

		return data;
	}

	private Map<String, Object> createExpectedMap() {

		Map<String, Object> data = new HashMap<>();

		data.put("string", "value");
		data.put("number", 1234);
		data.put("boolean", true);

		return data;
	}
}
