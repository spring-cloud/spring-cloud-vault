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
package org.springframework.cloud.vault.config.databases;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import org.springframework.cloud.vault.AbstractIntegrationTests;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultTemplate;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.Settings;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecureBackendAccessorFactory.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link VaultClient} using the postgresql secret backend. This
 * test requires a running PostgreSQL instance, see {@link #CONNECTION_URL}.
 *
 * @author Mark Paluch
 */
public class PostgreSqlSecretIntegrationTests extends AbstractIntegrationTests {

	private final static String POSTGRES_HOST = "localhost";
	private final static int POSTGRES_PORT = 5432;

	private final static String CONNECTION_URL = String.format(
			"postgresql://spring:vault@%s:%d/postgres?sslmode=disable", POSTGRES_HOST,
			POSTGRES_PORT);

	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE ROLE \"{{name}}\" WITH "
			+ "LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';\n"
			+ "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	private VaultPostgreSqlProperties postgreSql = new VaultPostgreSqlProperties();

	/**
	 * Initialize the postgresql secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(POSTGRES_HOST, POSTGRES_PORT)));

		postgreSql.setEnabled(true);
		postgreSql.setRole("readonly");

		if (!prepare().hasSecret(postgreSql.getBackend())) {
			prepare().mountSecret(postgreSql.getBackend());
		}

		prepare().write(String.format("%s/config/connection", postgreSql.getBackend()),
				Collections.singletonMap("connection_url", CONNECTION_URL));

		prepare().write(
				String.format("%s/roles/%s", postgreSql.getBackend(),
						postgreSql.getRole()),
				Collections.singletonMap("sql", CREATE_USER_AND_GRANT_SQL));

		configOperations = new VaultTemplate(vaultProperties, prepare().newVaultClient(),
				ClientAuthentication.token(vaultProperties)).opsForConfig();
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = configOperations
				.read(forDatabase(postgreSql));

		assertThat(secretProperties).containsKeys("spring.datasource.username",
				"spring.datasource.password");
	}

}
