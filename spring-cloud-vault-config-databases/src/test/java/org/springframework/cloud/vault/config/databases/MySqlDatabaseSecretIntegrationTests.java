/*
 * Copyright 2017-present the original author or authors.
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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;

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

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	@SuppressWarnings("deprecation")
	private VaultMySqlProperties mySql = new VaultMySqlProperties();

	/**
	 * Initialize the mysql secret backend.
	 */
	@BeforeEach
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MySqlFixtures.MYSQL_HOST, MySqlFixtures.MYSQL_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.7.1")));

		this.mySql.setEnabled(true);
		this.mySql.setRole("readonly");
		this.mySql.setBackend("database");

		MySqlFixtures.setupMysql(this.vaultRule);

		this.configOperations = new VaultConfigTemplate(this.vaultRule.prepare().getVaultOperations(),
				this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forDatabase(this.mySql)).getData();

		assertThat(secretProperties).containsKeys("spring.datasource.username", "spring.datasource.password");
	}

}
