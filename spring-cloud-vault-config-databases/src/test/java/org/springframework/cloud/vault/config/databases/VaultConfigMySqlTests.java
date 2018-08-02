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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;

import static org.junit.Assume.*;

/**
 * Integration tests using the mysql secret backend. In case this test should fail because
 * of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigMySqlTests.TestApplication.class, properties = {
		"spring.cloud.vault.mysql.enabled=true", "spring.cloud.vault.mysql.role=readonly",
		"spring.datasource.url=jdbc:mysql://localhost:3306/mysql?useSSL=false&serverTimezone=UTC",
		"spring.main.allow-bean-definition-overriding=true" })
public class VaultConfigMySqlTests {

	private final static int MYSQL_PORT = 3306;
	private final static String MYSQL_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String
			.format("springvault:springvault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);
	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	/**
	 * Initialize the mysql secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecretBackend("mysql")) {
			vaultRule.prepare().mountSecret("mysql");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		vaultOperations.write("mysql/config/connection",
				Collections.singletonMap("connection_url", ROOT_CREDENTIALS));

		vaultOperations.write("mysql/roles/readonly",
				Collections.singletonMap("sql", CREATE_USER_AND_GRANT_SQL));
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
