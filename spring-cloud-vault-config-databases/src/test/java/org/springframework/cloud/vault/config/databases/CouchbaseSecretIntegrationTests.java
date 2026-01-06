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

package org.springframework.cloud.vault.config.databases;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.forDatabase;

/**
 * Integration tests for {@link VaultConfigTemplate} using the couchbase secret backend.
 * This test requires a running Couchbase instance, see {@link #COUCHBASE_HOST}.
 *
 * @author Mark Paluch
 */
public class CouchbaseSecretIntegrationTests extends IntegrationTestSupport {

	private static final int COUCHBASE_PORT = 8091;

	private static final String COUCHBASE_HOST = "localhost";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultCouchbaseProperties couchbaseProperties = new VaultCouchbaseProperties();

	/**
	 * Initialize couchbase secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(COUCHBASE_HOST, COUCHBASE_PORT)));
		assumeTrue(this.vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("1.3.0")));

		this.couchbaseProperties.setEnabled(true);
		this.couchbaseProperties.setRole("couchbase-readonly");

		if (!prepare().hasSecretsEngine(this.couchbaseProperties.getBackend())) {
			prepare().mountSecretsEngine(this.couchbaseProperties.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "couchbase-database-plugin");
		config.put("hosts", "couchbase://localhost");
		config.put("username", "Administrator");
		config.put("password", "password");
		config.put("allowed_roles", "*");

		vaultOperations.write("database/config/spring-cloud-vault-couchbase", config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "spring-cloud-vault-couchbase");
		body.put("creation_statements", "{\"roles\":[{\"role\":\"ro_admin\"}]}");

		vaultOperations.write("database/roles/couchbase-readonly", body);

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forDatabase(this.couchbaseProperties))
			.getData();

		assertThat(secretProperties).containsKeys("spring.couchbase.username", "spring.couchbase.password");
	}

}
