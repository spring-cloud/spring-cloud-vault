/*
 * Copyright 2017-2020 the original author or authors.
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

import com.couchbase.client.java.Cluster;
import com.couchbase.client.core.error.UnambiguousTimeoutException;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.core.VaultOperations;

import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the database secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * Needs existing Couchbase user named vault-static with at least ro_admin privs.
 *
 * @author Francis Hitchens
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigCouchbaseDatabaseStaticTests.TestApplication.class,
		properties = { "spring.cloud.vault.couchbase.enabled=true",
				"spring.cloud.vault.couchbase.role=staticreadonly",
				"spring.cloud.vault.couchbase.staticRole=true",
				"spring.data.couchbase.username=foo",
				"spring.data.couchbase.password=bar",
				"spring.main.allow-bean-definition-overriding=true" })
public class VaultConfigCouchbaseDatabaseStaticTests {

	private static final int COUCHBASE_PORT = 8091;

	private static final String COUCHBASE_HOST = "localhost";

	@Value("${spring.data.couchbase.username}")
	String username;

	@Value("${spring.data.couchbase.password}")
	String password;

	Cluster cluster;

	/**
	 * Initialize the couchbase secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(CanConnect.to(new InetSocketAddress(COUCHBASE_HOST, COUCHBASE_PORT)));
		assumeTrue(vaultRule.prepare().getVersion()
				.isGreaterThanOrEqualTo(Version.parse("0.7.1")));

		if (!vaultRule.prepare().hasSecretBackend("database")) {
			vaultRule.prepare().mountSecret("database");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "couchbase-database-plugin");
		config.put("hosts", "couchbase://localhost");
		config.put("username", "Administrator");
		config.put("password", "Admin123");
		config.put("allowed_roles", "*");

		vaultOperations.write("database/config/spring-cloud-vault-couchbase", config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "spring-cloud-vault-couchbase");
		body.put("username", "vault-static");
		body.put("rotation_period", "5m");
		body.put("creation_statements", "[{\"name\":\"ro_admin\"}]");

		vaultOperations.write("database/static-roles/staticreadonly", body);
	}

	@Test
	public void shouldConnectConnection() throws UnambiguousTimeoutException {

		this.cluster = Cluster.connect("127.0.0.1", this.username, this.password);
		this.cluster.waitUntilReady(Duration.ofSeconds(5));
		this.cluster.disconnect();
	}

	@Test(expected = UnambiguousTimeoutException.class)
	public void shouldFailConnectConnection() throws UnambiguousTimeoutException {

		this.cluster = Cluster.connect("127.0.0.1", this.username, "fake.pwd");
		this.cluster.waitUntilReady(Duration.ofSeconds(5));
		this.cluster.disconnect();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
