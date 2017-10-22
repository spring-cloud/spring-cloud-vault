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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.Session;
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

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the cassandra secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 * 
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigDatabaseTests.TestApplication.class, properties = {
		"spring.cloud.vault.database.enabled=true",
		"spring.cloud.vault.database.role=readonly" })
public class VaultConfigDatabaseTests {

	private final static String CASSANDRA_HOST = "localhost";
	private final static int CASSANDRA_PORT = 9042;

	private final static String CASSANDRA_USERNAME = "springvault";
	private final static String CASSANDRA_PASSWORD = "springvault";

	private final static String CREATE_USER_AND_GRANT_CQL = "CREATE USER '{{username}}' WITH PASSWORD '{{password}}' NOSUPERUSER;"
			+ "GRANT SELECT ON ALL KEYSPACES TO {{username}};";
	private static final String ROLE_NAME = "readonly";
	private static final String BACKEND = "database";

	/**
	 * Initialize the database secret backend with the cassandra plugin.
	 *
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(CASSANDRA_HOST, CASSANDRA_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecretBackend(BACKEND)) {
			vaultRule.prepare().mountSecret(BACKEND);
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> connection = new HashMap<>();
		connection.put("plugin_name", "cassandra-database-plugin");
		connection.put("allowed_roles", ROLE_NAME);
		connection.put("hosts", CASSANDRA_HOST);
		connection.put("username", CASSANDRA_USERNAME);
		connection.put("password", CASSANDRA_PASSWORD);

		vaultOperations.write(String.format("%s/config/cassandra", BACKEND),
				connection);

		Map<String, String> role = new HashMap<>();

		role.put("creation_statements", CREATE_USER_AND_GRANT_CQL);
		role.put("db_name", "cassandra");

		vaultOperations.write(String.format("%s/roles/%s",BACKEND,ROLE_NAME), role);
	}

	@Value("${spring.data.cassandra.username}")
	String username;

	@Value("${spring.data.cassandra.password}")
	String password;

	@Autowired
	Cluster cluster;

	@Test
	public void shouldConnectUsingCluster() throws SQLException {
		cluster.connect().close();
	}

	@Test
	public void shouldUseAuthenticationSet() throws SQLException {
		assertThat(cluster.getConfiguration().getProtocolOptions().getAuthProvider())
				.isInstanceOf(PlainTextAuthProvider.class);
	}

	@Test
	public void shouldConnectUsingCassandraClient() throws SQLException {

		Cluster cluster = Cluster.builder().addContactPoint(CASSANDRA_HOST)
				.withAuthProvider(new PlainTextAuthProvider(username, password)).build();
		Session session = cluster.connect();
		session.close();
		cluster.close();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
