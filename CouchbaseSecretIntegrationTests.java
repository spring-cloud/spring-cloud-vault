/*
 * Copyright 2016-2020 the original author or authors.
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
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.forDatabase;

/**
 * Integration tests for {@link VaultConfigTemplate} using the couchbase secret backend.
 * This test requires a running Couchbase instance, see {@link #COUCHBASE_HOST} and other
 * {@code COUCHBASE_*} properties.
 *
 * @author Francis J. Hitchens
 */
public class CouchbaseSecretIntegrationTests extends IntegrationTestSupport {

	private static final String COUCHBASE_HOST = "localhost";

	private static final int COUCHBASE_PORT = 11210;

	private static final String COUCHBASE_USERNAME = "springvault";

	private static final String COUCHBASE_PASSWORD = "springvault";

	private static final String CREATE_USER_AND_GRANT_CQL = "CREATE USER '{{username}}' WITH PASSWORD '{{password}}' NOSUPERUSER;"
			+ "GRANT SELECT ON ALL KEYSPACES TO {{username}};";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultCouchbaseProperties couchbase = new VaultCouchbaseProperties();

	/**
	 * Initialize couchbase secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(COUCHBASE_HOST, COUCHBASE_PORT)));

		this.couchbase.setEnabled(true);
		this.couchbase.setRole("readonly");

		if (!prepare().hasSecretBackend(this.couchbase.getBackend())) {
			prepare().mountSecret(this.couchbase.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, Object> connection = new HashMap<>();
		connection.put("hosts", COUCHBASE_HOST);
		connection.put("username", COUCHBASE_USERNAME);
		connection.put("password", COUCHBASE_PASSWORD);
		connection.put("protocol_version", 3);

		vaultOperations.write(
				String.format("%s/config/connection", this.couchbase.getBackend()),
				connection);

		Map<String, String> role = new HashMap<>();

		role.put("creation_cql", CREATE_USER_AND_GRANT_CQL);
		role.put("consistency", "All");

		vaultOperations.write(String.format("%s/roles/%s", this.couchbase.getBackend(),
				this.couchbase.getRole()), role);

		this.configOperations = new VaultConfigTemplate(vaultOperations,
				this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations
				.read(forDatabase(this.couchbase)).getData();

		assertThat(secretProperties).containsKeys("spring.data.couchbase.username",
				"spring.data.couchbase.password");
	}

}
