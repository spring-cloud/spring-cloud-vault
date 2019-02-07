/*
 * Copyright 2017-2018 the original author or authors.
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
 * Integration tests for {@link VaultConfigTemplate} using the {@code database} secret
 * backend with {@code mysql-legacy-database-plugin}. This test requires a running MySQL
 * instance, see {@link #ROOT_CREDENTIALS}.
 *
 * @author Mark Paluch
 */
public class MySqlDatabaseSecretIntegrationTests extends IntegrationTestSupport {

	private static final int MYSQL_PORT = 3306;

	private static final String MYSQL_HOST = "localhost";

	private static final String ROOT_CREDENTIALS = String
			.format("springvault:springvault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);

	private static final String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	@SuppressWarnings("deprecation")
	private VaultMySqlProperties mySql = new VaultMySqlProperties();

	/**
	 * Initialize the mysql secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.7.1")));

		this.mySql.setEnabled(true);
		this.mySql.setRole("readonly");
		this.mySql.setBackend("database");

		if (!prepare().hasSecretBackend(this.mySql.getBackend())) {
			prepare().mountSecret(this.mySql.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "mysql-legacy-database-plugin");
		config.put("connection_url", ROOT_CREDENTIALS);
		config.put("allowed_roles", "readonly");

		vaultOperations.write(String.format("%s/config/mysql", this.mySql.getBackend()),
				config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "mysql");
		body.put("creation_statements", CREATE_USER_AND_GRANT_SQL);

		vaultOperations.write(String.format("%s/roles/%s", this.mySql.getBackend(),
				this.mySql.getRole()), body);

		this.configOperations = new VaultConfigTemplate(vaultOperations,
				this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations
				.read(forDatabase(this.mySql)).getData();

		assertThat(secretProperties).containsKeys("spring.datasource.username",
				"spring.datasource.password");
	}

}
