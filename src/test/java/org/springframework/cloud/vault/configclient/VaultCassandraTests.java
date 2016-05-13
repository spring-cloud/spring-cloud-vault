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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.*;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.Session;

/**
 * Integration tests using the cassandra secret backend.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VaultCassandraTests.TestApplication.class)
@IntegrationTest({ "spring.cloud.vault.cassandra.enabled=true",
		"spring.cloud.vault.cassandra.role=readonly" })
public class VaultCassandraTests {

	private final static String CASSANDRA_HOST = "localhost";
	private final static int CASSANDRA_PORT = 9042;

	private final static String CASSANDRA_USERNAME = "cassandra";
	private final static String CASSANDRA_PASSWORD = "cassandra";

	private final static String CREATE_USER_AND_GRANT_CQL = "CREATE USER '{{username}}' WITH PASSWORD '{{password}}' NOSUPERUSER;"
			+ "GRANT SELECT ON ALL KEYSPACES TO {{username}};";

	/**
	 * Initialize the cassandra secret backend.
	 *
	 * @throws Exception
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(CASSANDRA_HOST, CASSANDRA_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecret("cassandra")) {
			vaultRule.prepare().mountSecret("cassandra");
		}

		Map<String, String> connection = new HashMap<>();
		connection.put("hosts", CASSANDRA_HOST);
		connection.put("username", CASSANDRA_USERNAME);
		connection.put("password", CASSANDRA_PASSWORD);

		vaultRule.prepare().write(String.format("%s/config/connection", "cassandra"),
				connection);

		vaultRule.prepare().write("cassandra/roles/readonly",
				Collections.singletonMap("creation_cql", CREATE_USER_AND_GRANT_CQL));
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
		assertThat(cluster.getConfiguration().getProtocolOptions().getAuthProvider()).isInstanceOf(PlainTextAuthProvider.class);
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
