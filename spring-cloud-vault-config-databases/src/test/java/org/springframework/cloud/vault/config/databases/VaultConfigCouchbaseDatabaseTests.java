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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.core.error.UnambiguousTimeoutException;
import com.couchbase.client.java.Cluster;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the database secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Francis Hitchens
 */
@SpringBootTest(classes = VaultConfigCouchbaseDatabaseTests.TestApplication.class,
		properties = { "spring.cloud.vault.couchbase.enabled=true", "spring.config.import=vault://",
				"spring.cloud.vault.couchbase.role=couchbase-readonly",
				"spring.main.allow-bean-definition-overriding=true" })
public class VaultConfigCouchbaseDatabaseTests {

	private static final int COUCHBASE_PORT = 8091;

	private static final String COUCHBASE_HOST = "localhost";

	@Value("${spring.couchbase.username}")
	String username;

	@Value("${spring.couchbase.password}")
	String password;

	Cluster cluster;

	/**
	 * Initialize the couchbase secret backend.
	 */
	@BeforeAll
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(CanConnect.to(new InetSocketAddress(COUCHBASE_HOST, COUCHBASE_PORT)));
		assumeTrue(vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("1.3.0")));

		if (!vaultRule.prepare().hasSecretsEngine("database")) {
			vaultRule.prepare().mountSecretsEngine("database");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "couchbase-database-plugin");
		config.put("hosts", "couchbase://localhost");
		config.put("username", "Administrator");
		config.put("password", "password");
		config.put("allowed_roles", "*");

		vaultOperations.write("database/config/spring-cloud-vault-couchbase", config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "spring-cloud-vault-couchbase");
		body.put("creation_statements", "{\"roles\":[{\"role\":\"ro_admin\"}]}");

		vaultOperations.write("database/roles/couchbase-readonly", body);
	}

	@Test
	public void shouldConnectConnection() throws UnambiguousTimeoutException {

		this.cluster = Cluster.connect("127.0.0.1", this.username, this.password);
		this.cluster.waitUntilReady(Duration.ofSeconds(5));
		this.cluster.disconnect();
	}

	@Test
	public void shouldFailConnectConnection() throws UnambiguousTimeoutException {

		this.cluster = Cluster.connect("127.0.0.1", this.username, "fake.pwd");

		assertThatExceptionOfType(UnambiguousTimeoutException.class).isThrownBy(() -> {

			this.cluster.waitUntilReady(Duration.ofSeconds(5));
			this.cluster.disconnect();
		});
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
