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
package org.springframework.cloud.vault.config.databases;

import java.net.InetSocketAddress;
import java.util.Collections;
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.*;

/**
 * Integration tests for
 * {@link org.springframework.cloud.vault.config.VaultConfigTemplate} using the postgresql
 * secret backend. This test requires a running PostgreSQL instance, see
 * {@link #CONNECTION_URL}.
 *
 * @author Mark Paluch
 */
public class PostgreSqlSecretIntegrationTests extends IntegrationTestSupport {

	private final static String POSTGRES_HOST = "localhost";
	private final static int POSTGRES_PORT = 5432;

	private final static String CONNECTION_URL = String.format(
			"postgresql://springvault:springvault@%s:%d/postgres?sslmode=disable", POSTGRES_HOST,
			POSTGRES_PORT);

	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE ROLE \"{{name}}\" WITH "
			+ "LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';\n"
			+ "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	@SuppressWarnings("deprecation")
	private VaultPostgreSqlProperties postgreSql = new VaultPostgreSqlProperties();

	/**
	 * Initialize the postgresql secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(POSTGRES_HOST, POSTGRES_PORT)));

		postgreSql.setEnabled(true);
		postgreSql.setRole("readonly");

		if (!prepare().hasSecretBackend(postgreSql.getBackend())) {
			prepare().mountSecret(postgreSql.getBackend());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		vaultOperations.write(
				String.format("%s/config/connection", postgreSql.getBackend()),
				Collections.singletonMap("connection_url", CONNECTION_URL));

		vaultOperations.write(
				String.format("%s/roles/%s", postgreSql.getBackend(),
						postgreSql.getRole()),
				Collections.singletonMap("sql", CREATE_USER_AND_GRANT_SQL));

		configOperations = new VaultConfigTemplate(vaultOperations, vaultProperties);

	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = configOperations
				.read(forDatabase(postgreSql)).getData();

		assertThat(secretProperties).containsKeys("spring.datasource.username",
				"spring.datasource.password");
	}
}
