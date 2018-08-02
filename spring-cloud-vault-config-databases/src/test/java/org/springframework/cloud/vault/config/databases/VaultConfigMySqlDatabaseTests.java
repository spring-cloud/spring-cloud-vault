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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;

import static org.junit.Assume.*;

/**
 * Integration tests using the database secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigMySqlDatabaseTests.TestApplication.class, properties = {
		"spring.cloud.vault.database.enabled=true",
		"spring.cloud.vault.database.role=readonly",
		"spring.datasource.url=jdbc:mysql://localhost:3306/mysql?useSSL=false&serverTimezone=UTC",
		"spring.main.allow-bean-definition-overriding=true" })
public class VaultConfigMySqlDatabaseTests {

	private final static int MYSQL_PORT = 3306;
	private final static String MYSQL_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String.format(
			"springvault:springvault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);
	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	/**
	 * Initialize the mysql secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));
		assumeTrue(vaultRule.prepare().getVersion()
				.isGreaterThanOrEqualTo(Version.parse("0.7.1")));

		if (!vaultRule.prepare().hasSecretBackend("database")) {
			vaultRule.prepare().mountSecret("database");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "mysql-legacy-database-plugin");
		config.put("connection_url", ROOT_CREDENTIALS);
		config.put("allowed_roles", "readonly");

		vaultOperations.write("database/config/mysql", config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "mysql");
		body.put("creation_statements", CREATE_USER_AND_GRANT_SQL);

		vaultOperations.write("database/roles/readonly", body);
	}

	@Value("${spring.datasource.username}")
	String username;

	@Value("${spring.datasource.password}")
	String password;

	@Autowired
	DataSource dataSource;

	@Test
	public void shouldConnectUsingDataSource() throws SQLException {

		dataSource.getConnection().close();
	}

	@Test
	public void shouldConnectUsingJdbcUrlConnection() throws SQLException {

		String url = String.format("jdbc:mysql://%s?useSSL=false&serverTimezone=UTC", MYSQL_HOST);
		DriverManager.getConnection(url, username, password).close();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
