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
package org.springframework.cloud.vault;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.SecureBackendAccessors.*;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.Settings;

/**
 * Integration tests for {@link VaultClient} using the rabbitmq secret backend. This test
 * requires a running RabbitMQ instance, see {@link #RABBITMQ_URI}.
 *
 * @author Mark Paluch
 */
public class RabbitMQSecretIntegrationTests extends AbstractIntegrationTests {

	private final static int RABBITMQ_HTTP_MANAGEMENT_PORT = 15672;
	private final static String RABBITMQ_HOST = "localhost";

	private final static String RABBITMQ_USERNAME = "guest";
	private final static String RABBITMQ_PASSWORD = "guest";

	private final static String RABBITMQ_URI = String
			.format("http://%s:%d", RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT);

	private final static String VHOSTS_ROLE = "{\"/\":{\"write\": \".*\", \"read\": \".*\"}}";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultClient vaultClient = new VaultClient(vaultProperties);
	private VaultProperties.Rabbitmq rabbitmq = vaultProperties.getRabbitmq();

	/**
	 * Initialize the mysql secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT)));

		rabbitmq.setEnabled(true);
		rabbitmq.setRole("readonly");

		if (!prepare().hasSecret(rabbitmq.getBackend())) {
			prepare().mountSecret(rabbitmq.getBackend());
		}

		Map<String, String> connection = new HashMap<>();
		connection.put("connection_uri", RABBITMQ_URI);
		connection.put("username", RABBITMQ_USERNAME);
		connection.put("password", RABBITMQ_PASSWORD);

		prepare().write(String.format("%s/config/connection", rabbitmq.getBackend()),
				connection);

		prepare().write(String.format("%s/roles/%s", rabbitmq.getBackend(), rabbitmq.getRole()),
				Collections.singletonMap("vhosts", VHOSTS_ROLE));

		vaultClient.setRest(TestRestTemplateFactory.create(vaultProperties));
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = vaultClient.read(database(rabbitmq),
				Settings.token());

		assertThat(secretProperties).containsKeys("spring.rabbitmq.username",
				"spring.rabbitmq.password");
	}

}
