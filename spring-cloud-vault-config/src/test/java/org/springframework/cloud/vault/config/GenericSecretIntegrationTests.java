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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.cloud.vault.config.SecureBackendAccessors.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.vault.AbstractIntegrationTests;
import org.springframework.cloud.vault.TestRestTemplateFactory;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.cloud.vault.util.Settings;

/**
 * Integration tests for {@link VaultClient} using the generic secret backend.
 *
 * @author Mark Paluch
 */
public class GenericSecretIntegrationTests extends AbstractIntegrationTests {

	protected VaultProperties vaultProperties = Settings.createVaultProperties();
	protected VaultClient vaultClient = new VaultClient(vaultProperties);

	@Before
	public void setUp() throws Exception {

		vaultProperties.setFailFast(false);
		prepare().writeSecret("app-name", (Map) createData());
		vaultClient.setRest(TestRestTemplateFactory.create(vaultProperties));
	}

	@Test
	public void shouldReturnSecretsCorrectly() throws Exception {

		Map<String, String> secretProperties = vaultClient
				.read(generic("secret", "app-name"), createToken());

		assertThat(secretProperties).containsAllEntriesOf(createExpectedMap());
	}

	@Test
	public void shouldReturnNullIfNotFound() throws Exception {

		Map<String, String> secretProperties = vaultClient
				.read(generic("secret", "missing"), createToken());

		assertThat(secretProperties).isEmpty();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailOnFailFast() throws Exception {

		vaultProperties.setFailFast(true);
		vaultClient.read(generic("secret", "missing"), createToken());
	}

	/**
	 * Can be overridden by subclasses.
	 * 
	 * @return
	 */
	protected VaultToken createToken() {
		return Settings.token();
	}

	private Map<String, Object> createData() {
		Map<String, Object> data = new HashMap<>();
		data.put("string", "value");
		data.put("number", "1234");
		data.put("boolean", true);
		return data;
	}

	private Map<String, String> createExpectedMap() {
		Map<String, String> data = new HashMap<>();
		data.put("string", "value");
		data.put("number", "1234");
		data.put("boolean", "true");
		return data;
	}
}
