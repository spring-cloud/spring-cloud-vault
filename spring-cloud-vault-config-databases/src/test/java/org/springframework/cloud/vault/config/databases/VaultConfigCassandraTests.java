/*
 * Copyright 2016-present the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.vault.core.VaultOperations;

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

@SpringBootTest(classes = VaultConfigCassandraTests.TestApplication.class,
		properties = { "spring.cloud.vault.cassandra.enabled=true", "spring.cloud.vault.cassandra.role=readonly",
				"spring.data.cassandra.jmx-enabled=false", "spring.cloud.bootstrap.enabled=true" })
public class VaultConfigCassandraTests {

	private static final String CASSANDRA_HOST = "localhost";

	private static final int CASSANDRA_PORT = 9042;

	private static final String CASSANDRA_USERNAME = "springvault";

	private static final String CASSANDRA_PASSWORD = "springvault";

	private static final String CREATE_USER_AND_GRANT_CQL = "CREATE USER '{{username}}' WITH PASSWORD '{{password}}' NOSUPERUSER;"
			+ "GRANT SELECT ON ALL KEYSPACES TO {{username}};";

	@Value("${spring.data.cassandra.username}")
	String username;

	@Value("${spring.data.cassandra.password}")
	String password;

	@Autowired
	CqlSession cqlSession;

	/**
	 * Initialize the cassandra secret backend.
	 */
	@BeforeAll
	public static void beforeClass() {

		assumeTrue(CanConnect.to(new InetSocketAddress(CASSANDRA_HOST, CASSANDRA_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecretsEngine("cassandra")) {
			vaultRule.prepare().mountSecretsEngine("cassandra");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, Object> connection = new HashMap<>();
		connection.put("hosts", CASSANDRA_HOST);
		connection.put("username", CASSANDRA_USERNAME);
		connection.put("password", CASSANDRA_PASSWORD);
		connection.put("protocol_version", 3);

		vaultOperations.write(String.format("%s/config/connection", "cassandra"), connection);

		Map<String, String> role = new HashMap<>();

		role.put("creation_cql", CREATE_USER_AND_GRANT_CQL);
		role.put("consistency", "All");

		vaultOperations.write("cassandra/roles/readonly", role);
	}

	@Test
	public void shouldUseAuthenticatedSession() {
		assertThat(this.cqlSession.getMetadata().getKeyspace("system")).isNotEmpty();
	}

	@Test
	public void shouldConnectUsingCassandraClient() {

		try (CqlSession session = CqlSession.builder()
			.withLocalDatacenter("dc1")
			.addContactPoint(new InetSocketAddress(CASSANDRA_HOST, CASSANDRA_PORT))
			.withAuthCredentials(this.username, this.password)
			.build()) {
			assertThat(session.getMetadata().getKeyspace("system")).isNotEmpty();
		}
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
