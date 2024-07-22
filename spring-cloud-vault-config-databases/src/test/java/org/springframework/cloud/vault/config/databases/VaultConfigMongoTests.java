/*
 * Copyright 2016-2021 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
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
import org.springframework.vault.core.VaultOperations;

import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the mongodb secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigMongoTests.TestApplication.class,
		properties = { "spring.cloud.vault.mongodb.enabled=true", "spring.cloud.vault.mongodb.role=readonly",
				"spring.data.mongodb.url=mongodb://localhost", "spring.data.mongodb.database=admin",
				"spring.cloud.bootstrap.enabled=true" })
public class VaultConfigMongoTests {

	private static final int MONGODB_PORT = 27017;

	private static final String MONGODB_HOST = "localhost";

	private static final String ROOT_CREDENTIALS = String
		.format("mongodb://springvault:springvault@%s:%d/admin?ssl=false", MONGODB_HOST, MONGODB_PORT);

	private static final String ROLES = "[ \"readWrite\", { \"role\": \"read\", \"db\": \"admin\" } ]";

	@Value("${spring.data.mongodb.username}")
	String username;

	@Value("${spring.data.mongodb.password}")
	String password;

	@Autowired
	MongoClient mongoClient;

	/**
	 * Initialize the mongo secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MONGODB_HOST, MONGODB_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		if (!vaultRule.prepare().hasSecretBackend("mongodb")) {
			vaultRule.prepare().mountSecret("mongodb");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		vaultOperations.write("mongodb/config/connection", Collections.singletonMap("uri", ROOT_CREDENTIALS));

		Map<String, String> role = new HashMap<>();
		role.put("db", "admin");
		role.put("roles", ROLES);

		vaultOperations.write("mongodb/roles/readonly", role);
	}

	@Test
	public void shouldConnectUsingDataSource() {

		MongoDatabase mongoDatabase = this.mongoClient.getDatabase("admin");

		List<Document> collections = mongoDatabase.listCollections().into(new ArrayList<>());

		for (Document collection : collections) {
			if (collection.getString("name").equals("hello")) {
				mongoDatabase.getCollection(collection.getString("name")).drop();
			}
		}

		mongoDatabase.createCollection("hello");
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args).registerShutdownHook();
		}

	}

}
