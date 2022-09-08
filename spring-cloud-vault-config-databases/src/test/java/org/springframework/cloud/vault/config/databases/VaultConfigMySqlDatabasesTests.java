/*
 * Copyright 2017-2021 the original author or authors.
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
import java.sql.DriverManager;
import java.sql.SQLException;

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
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the database secret backend with multi-database support. In
 * case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigMySqlDatabasesTests.TestApplication.class,
		properties = { "spring.cloud.vault.databases.mysql.enabled=true",
				"spring.cloud.vault.databases.mysql.role=readonly", "spring.datasource.url=" + MySqlFixtures.JDBC_URL,
				"spring.config.import=vault://" })
public class VaultConfigMySqlDatabasesTests {

	@Value("${spring.datasource.username}")
	String username;

	@Value("${spring.datasource.password}")
	String password;

	@Autowired
	DataSource dataSource;

	/**
	 * Initialize the mysql secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(CanConnect.to(new InetSocketAddress(MySqlFixtures.MYSQL_HOST, MySqlFixtures.MYSQL_PORT)));
		assumeTrue(vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.7.1")));

		MySqlFixtures.setupMysql(vaultRule);
	}

	@Test
	public void shouldConnectUsingDataSource() throws SQLException {

		this.dataSource.getConnection().close();
	}

	@Test
	public void shouldConnectUsingJdbcUrlConnection() throws SQLException {
		DriverManager.getConnection(MySqlFixtures.JDBC_URL, this.username, this.password).close();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
