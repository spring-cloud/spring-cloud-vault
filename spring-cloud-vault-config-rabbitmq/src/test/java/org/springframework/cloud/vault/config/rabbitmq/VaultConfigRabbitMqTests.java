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

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;

import static org.junit.Assume.*;

/**
 * Integration tests using the rabbitmq secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigRabbitMqTests.TestApplication.class, properties = {
		"spring.cloud.vault.rabbitmq.enabled=true",
		"spring.cloud.vault.rabbitmq.role=readonly",
		"spring.rabbitmq.address=localhost" })
public class VaultConfigRabbitMqTests {

	private final static int RABBITMQ_HTTP_MANAGEMENT_PORT = 15672;
	private final static int RABBITMQ_PORT = 5672;
	private final static String RABBITMQ_HOST = "localhost";

	private final static String RABBITMQ_USERNAME = "guest";
	private final static String RABBITMQ_PASSWORD = "guest";

	private final static String RABBITMQ_URI = String.format("http://%s:%d",
			RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT);

	private final static String VHOSTS_ROLE = "{\"/\":{\"write\": \".*\", \"read\": \".*\"}}";

	/**
	 * Initialize the rabbitmq secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		assumeTrue(CanConnect
				.to(new InetSocketAddress(RABBITMQ_HOST, RABBITMQ_HTTP_MANAGEMENT_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion()
				.isGreaterThanOrEqualTo(Version.parse("0.6.2")));

		if (!vaultRule.prepare().hasSecretBackend("rabbitmq")) {
			vaultRule.prepare().mountSecret("rabbitmq");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> connection = new HashMap<>();
		connection.put("connection_uri", RABBITMQ_URI);
		connection.put("username", RABBITMQ_USERNAME);
		connection.put("password", RABBITMQ_PASSWORD);

		vaultOperations.write(String.format("rabbitmq/config/connection"), connection);

		vaultOperations.write(String.format("rabbitmq/roles/readonly"),
				Collections.singletonMap("vhosts", VHOSTS_ROLE));
	}

	@Value("${spring.rabbitmq.username}")
	String username;

	@Value("${spring.rabbitmq.password}")
	String password;

	@Autowired
	org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

	@Test
	public void shouldConnectSpringConnectionFactory() {
		connectionFactory.createConnection().close();
	}

	@Test
	public void shouldConnectUsingRabbitMQClient() throws Exception {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(RABBITMQ_HOST);
		factory.setPort(RABBITMQ_PORT);
		factory.setUsername(username);
		factory.setPassword(password);

		try (Connection connection = factory.newConnection()) {
			connection.createChannel().close();
		}
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
