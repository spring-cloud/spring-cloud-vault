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
package org.springframework.cloud.vault.config.rabbitmq;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.rabbitmq.VaultConfigRabbitMqBootstrapConfiguration.RabbitMqSecretBackendMetadataFactory.*;

/**
 * Integration tests for {@link VaultConfigTemplate} using the rabbitmq secret backend.
 * This test requires a running RabbitMQ instance, see {@link #RABBITMQ_URI}.
 *
 * @author Mark Paluch
 */
public class RabbitMqSecretIntegrationTests extends IntegrationTestSupport {

	private final static int RABBITMQ_HTTP_MANAGEMENT_PORT = 15672;
	private final static String RABBITMQ_HOST = "localhost";

	private final static String RABBITMQ_USERNAME = "guest";
	private final static String RABBITMQ_PASSWORD = "guest";

	private final static String RABBITMQ_URI = String.format("http://%s:%d",
			RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT);

	private final static String VHOSTS_ROLE = "{\"/\":{\"write\": \".*\", \"read\": \".*\"}}";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigTemplate configOperations;
	private VaultRabbitMqProperties rabbitmq = new VaultRabbitMqProperties();

	/**
	 * Initialize the rabbitmq secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect
				.to(new InetSocketAddress(RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		rabbitmq.setEnabled(true);
		rabbitmq.setRole("readonly");

		if (!prepare().hasSecretBackend(rabbitmq.getBackend())) {
			prepare().mountSecret(rabbitmq.getBackend());
		}

		Map<String, String> connection = new HashMap<>();
		connection.put("connection_uri", RABBITMQ_URI);
		connection.put("username", RABBITMQ_USERNAME);
		connection.put("password", RABBITMQ_PASSWORD);

		VaultOperations vaultOperations = prepare().getVaultOperations();

		vaultOperations.write(
				String.format("%s/config/connection", rabbitmq.getBackend()), connection);

		vaultOperations.write(
				String.format("%s/roles/%s", rabbitmq.getBackend(), rabbitmq.getRole()),
				Collections.singletonMap("vhosts", VHOSTS_ROLE));

		configOperations = new VaultConfigTemplate(vaultOperations, vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = configOperations
				.read(forRabbitMq(rabbitmq)).getData();

		assertThat(secretProperties).containsKeys("spring.rabbitmq.username",
				"spring.rabbitmq.password");
	}
}
