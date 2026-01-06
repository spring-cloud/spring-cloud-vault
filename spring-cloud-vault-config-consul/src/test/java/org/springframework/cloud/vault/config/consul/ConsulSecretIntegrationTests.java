/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.vault.config.consul;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.config.consul.VaultConfigConsulBootstrapConfiguration.ConsulSecretBackendMetadataFactory;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests for {@link VaultConfigTemplate} using the consul secret backend. This
 * test requires a running Consul instance, see {@link #CONNECTION_URL}.
 *
 * @author Mark Paluch
 */
public class ConsulSecretIntegrationTests extends IntegrationTestSupport {

	private static final String POLICY = "key \"\" { policy = \"read\" }";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultConsulProperties consul = new VaultConsulProperties();

	/**
	 * Initialize the consul secret backend.
	 */
	@BeforeEach
	public void setUp() {

		assumeTrue(SetupConsul.isConsulAvailable());

		this.consul.setEnabled(true);
		this.consul.setRole("readonly");

		if (!prepare().hasSecretsEngine(this.consul.getBackend())) {
			prepare().mountSecretsEngine(this.consul.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		SetupConsul.setupConsul(vaultOperations, this.consul.getBackend());

		vaultOperations.write(String.format("%s/roles/%s", this.consul.getBackend(), this.consul.getRole()),
				Collections.singletonMap("policy", Base64.getEncoder().encodeToString(POLICY.getBytes())));

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		ConsulSecretBackendMetadataFactory factory = new ConsulSecretBackendMetadataFactory(null);
		Map<String, Object> secretProperties = this.configOperations.read(factory.forConsul(this.consul)).getData();

		assertThat(secretProperties).containsKeys("spring.cloud.consul.config.acl-token",
				"spring.cloud.consul.discovery.acl-token");
	}

}
