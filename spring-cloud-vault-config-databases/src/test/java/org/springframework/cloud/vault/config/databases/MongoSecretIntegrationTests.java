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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.forDatabase;

/**
 * Integration tests for {@link VaultConfigTemplate} using the mongodb secret backend.
 * This test requires a running MongoDB instance, see {@link #ROOT_CREDENTIALS}.
 *
 * @author Mark Paluch
 */
public class MongoSecretIntegrationTests extends IntegrationTestSupport {

	private static final int MONGODB_PORT = 27017;

	private static final String MONGODB_HOST = "localhost";

	private static final String ROOT_CREDENTIALS = String
		.format("mongodb://springvault:springvault@%s:%d/admin?ssl=false", MONGODB_HOST, MONGODB_PORT);

	private static final String ROLES = "[ \"readWrite\", { \"role\": \"read\", \"db\": \"admin\" } ]";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultMongoProperties mongodb = new VaultMongoProperties();

	/**
	 * Initialize the mongodb secret backend.
	 */
	@BeforeEach
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(MONGODB_HOST, MONGODB_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		this.mongodb.setEnabled(true);
		this.mongodb.setRole("readonly");

		if (!prepare().hasSecretBackend(this.mongodb.getBackend())) {
			prepare().mountSecret(this.mongodb.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		vaultOperations.write(String.format("%s/config/connection", this.mongodb.getBackend()),
				Collections.singletonMap("uri", ROOT_CREDENTIALS));

		Map<String, String> role = new HashMap<>();
		role.put("db", "admin");
		role.put("roles", ROLES);

		vaultOperations.write(String.format("%s/roles/%s", this.mongodb.getBackend(), this.mongodb.getRole()), role);

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forDatabase(this.mongodb)).getData();

		assertThat(secretProperties).containsKeys("spring.data.mongodb.username", "spring.data.mongodb.password");
	}

}
