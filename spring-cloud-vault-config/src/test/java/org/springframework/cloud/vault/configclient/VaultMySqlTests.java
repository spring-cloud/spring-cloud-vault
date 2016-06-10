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

package org.springframework.cloud.vault.configclient;

import static org.junit.Assume.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import com.mysql.jdbc.MySQLConnection;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests using the mysql secret backend.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VaultMySqlTests.TestApplication.class)
@IntegrationTest({ "spring.cloud.vault.mysql.enabled=true",
		"spring.cloud.vault.mysql.role=readonly",
		"spring.datasource.url=jdbc:mysql://localhost:3306/mysql?useSSL=false" })
public class VaultMySqlTests {

	private final static int MYSQL_PORT = 3306;
	private final static String MYSQL_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String
			.format("spring:vault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);
	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	/**
	 * Initialize the mysql secret backend.
	 *
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecret("mysql")) {
			vaultRule.prepare().mountSecret("mysql");
		}

		vaultRule.prepare().write("mysql/config/connection",
				Collections.singletonMap("connection_url", ROOT_CREDENTIALS));

		vaultRule.prepare().write("mysql/roles/readonly",
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

		String url = String.format("jdbc:mysql://%s?useSSL=false", MYSQL_HOST);
		DriverManager.getConnection(url, username, password).close();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
