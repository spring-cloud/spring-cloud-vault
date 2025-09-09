/*
 * Copyright 2018-present the original author or authors.
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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests for {@link VaultConfigTemplate}.
 *
 * @author Mark Paluch
 */
public class VaultConfigTemplateIntegrationTests extends IntegrationTestSupport {

	@Before
	public void before() {
		prepare().getVaultOperations().write("secret/myapp", Collections.singletonMap("key", "value"));
	}

	@Test
	public void shouldReadValue() {

		VaultProperties vaultProperties = Settings.createVaultProperties();

		VaultConfigTemplate template = new VaultConfigTemplate(prepare().getVaultOperations(), vaultProperties);

		Secrets secrets = template.read(KeyValueSecretBackendMetadata.create("secret", "myapp"));

		assertThat(secrets.getData()).containsEntry("key", "value");
	}

	@Test
	public void shouldReadVersionedValue() {

		assumeTrue(this.vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.10.0")));

		this.vaultRule.prepare()
			.getVaultOperations()
			.write("versioned/data/testVaultApp",
					Collections.singletonMap("data", Collections.singletonMap("key", "value")));

		VaultProperties vaultProperties = Settings.createVaultProperties();

		VaultConfigTemplate template = new VaultConfigTemplate(prepare().getVaultOperations(), vaultProperties);

		Secrets secrets = template.read(KeyValueSecretBackendMetadata.create("versioned", "testVaultApp"));

		assertThat(secrets.getData()).containsEntry("key", "value");
	}

}
