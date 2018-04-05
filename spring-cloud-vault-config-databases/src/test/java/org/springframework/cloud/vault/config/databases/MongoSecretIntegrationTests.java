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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.*;

/**
 * Integration tests for {@link VaultConfigTemplate} using the mongodb secret backend.
 * This test requires a running MongoDB instance, see {@link #ROOT_CREDENTIALS}.
 *
 * @author Mark Paluch
 */
public class MongoSecretIntegrationTests extends IntegrationTestSupport {

	private final static int MONGODB_PORT = 27017;
	private final static String MONGODB_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String.format(
			"mongodb://springvault:springvault@%s:%d/admin?ssl=false", MONGODB_HOST, MONGODB_PORT);
	private final static String ROLES = "[ \"readWrite\", { \"role\": \"read\", \"db\": \"admin\" } ]";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	private VaultMongoProperties mongodb = new VaultMongoProperties();

	/**
	 * Initialize the mongodb secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MONGODB_HOST, MONGODB_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		mongodb.setEnabled(true);
		mongodb.setRole("readonly");

		if (!prepare().hasSecretBackend(mongodb.getBackend())) {
			prepare().mountSecret(mongodb.getBackend());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		vaultOperations.write(String.format("%s/config/connection", mongodb.getBackend()),
				Collections.singletonMap("uri", ROOT_CREDENTIALS));

		Map<String, String> role = new HashMap<>();
		role.put("db", "admin");
		role.put("roles", ROLES);

		vaultOperations.write(
				String.format("%s/roles/%s", mongodb.getBackend(), mongodb.getRole()),
				role);

		configOperations = new VaultConfigTemplate(vaultOperations, vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = configOperations
				.read(forDatabase(mongodb))
				.getData();

		assertThat(secretProperties).containsKeys("spring.data.mongodb.username",
				"spring.data.mongodb.password");
	}
}
