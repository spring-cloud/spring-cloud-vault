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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.forDatabase;

/**
 * Integration tests for {@link VaultConfigTemplate} using the cassandra secret backend.
 * This test requires a running Cassandra instance, see {@link #CASSANDRA_HOST} and other
 * {@code CASSANDRA_*} properties.
 *
 * @author Mark Paluch
 */
public class CassandraSecretIntegrationTests extends IntegrationTestSupport {

	private static final String CASSANDRA_HOST = "localhost";

	private static final int CASSANDRA_PORT = 9042;

	private static final String CASSANDRA_USERNAME = "springvault";

	private static final String CASSANDRA_PASSWORD = "springvault";

	private static final String CREATE_USER_AND_GRANT_CQL = "CREATE USER '{{username}}' WITH PASSWORD '{{password}}' NOSUPERUSER;"
			+ "GRANT SELECT ON ALL KEYSPACES TO {{username}};";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultCassandraProperties cassandra = new VaultCassandraProperties();

	/**
	 * Initialize cassandra secret backend.
	 */
	@BeforeEach
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(CASSANDRA_HOST, CASSANDRA_PORT)));

		this.cassandra.setEnabled(true);
		this.cassandra.setRole("readonly");

		if (!prepare().hasSecretsEngine(this.cassandra.getBackend())) {
			prepare().mountSecretsEngine(this.cassandra.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, Object> connection = new HashMap<>();
		connection.put("hosts", CASSANDRA_HOST);
		connection.put("username", CASSANDRA_USERNAME);
		connection.put("password", CASSANDRA_PASSWORD);
		connection.put("protocol_version", 3);

		vaultOperations.write(String.format("%s/config/connection", this.cassandra.getBackend()), connection);

		Map<String, String> role = new HashMap<>();

		role.put("creation_cql", CREATE_USER_AND_GRANT_CQL);
		role.put("consistency", "All");

		vaultOperations.write(String.format("%s/roles/%s", this.cassandra.getBackend(), this.cassandra.getRole()),
				role);

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forDatabase(this.cassandra)).getData();

		assertThat(secretProperties).containsKeys("spring.data.cassandra.username", "spring.data.cassandra.password");
	}

}
