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

package org.springframework.cloud.vault.config;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultMount;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using config infrastructure with RADIUS authentication.
 *
 * <p>
 * This test uses Testcontainers to run both Vault and FreeRADIUS in a shared Docker
 * network. Testcontainers is required here because Vault's RADIUS auth method uses UDP
 * protocol for authentication. UDP port forwarding from Docker to the host is unreliable,
 * so both containers must be in the same network to communicate directly.
 *
 * @author Issam El-atif
 */
@Testcontainers
class VaultConfigRadiusTests {

	private static final String VAULT_TOKEN = "00000000-0000-0000-0000-000000000000";

	private static final String RADIUS_USERNAME = "testuser";

	private static final String RADIUS_PASSWORD = "testpass";

	private static final String RADIUS_SECRET = "testing123";

	private static final String RADIUS_ALIAS = "freeradius";

	static Network network = Network.newNetwork();

	// FreeRADIUS users config with test user
	private static final String USERS_CONFIG = "%s Cleartext-Password := \"%s\"%n".formatted(RADIUS_USERNAME,
			RADIUS_PASSWORD);

	// Custom FreeRADIUS clients config allowing any client
	private static final String CLIENTS_CONFIG = """
			client localhost {
				ipaddr = 127.0.0.1
				secret = testing123
			}
			client any {
				ipaddr = 0.0.0.0/0
				secret = %s
			}
			""".formatted(RADIUS_SECRET);

	@Container
	static GenericContainer<?> radiusContainer = new GenericContainer<>("freeradius/freeradius-server:latest")
		.withNetwork(network)
		.withNetworkAliases(RADIUS_ALIAS)
		.withCommand("-X")
		.withCopyToContainer(Transferable.of(USERS_CONFIG), "/etc/freeradius/mods-config/files/authorize")
		.withCopyToContainer(Transferable.of(CLIENTS_CONFIG), "/etc/freeradius/clients.conf")
		.waitingFor(Wait.forLogMessage(".*Ready to process requests.*\\n", 1));

	@Container
	static VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3").withVaultToken(VAULT_TOKEN)
		.withNetwork(network);

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigRadiusTests.class)
		.withAuthentication(VaultProperties.AuthenticationMethod.RADIUS)
		.withConfiguration(TestApplication.class)
		.withProperties("spring.cloud.vault.uri", vaultContainer.getHttpHostAddress())
		.withProperties("spring.cloud.vault.radius.username", RADIUS_USERNAME)
		.withProperties("spring.cloud.vault.radius.password", RADIUS_PASSWORD)
		.withSettings(VaultTestContextRunner.TestSettings::bootstrap);

	@BeforeAll
	static void beforeClass() {
		VaultTemplate vaultTemplate = createVaultTemplate();
		configureVaultSecrets(vaultTemplate);
		configureRadiusAuth(vaultTemplate);
	}

	private static VaultTemplate createVaultTemplate() {
		VaultEndpoint endpoint = VaultEndpoint.from(vaultContainer.getHttpHostAddress());
		return new VaultTemplate(endpoint, new SimpleClientHttpRequestFactory(),
				() -> new TokenAuthentication(VAULT_TOKEN).login());
	}

	private static void configureVaultSecrets(VaultTemplate vaultTemplate) {
		vaultTemplate.opsForSys().unmount("secret");
		vaultTemplate.opsForSys()
			.mount("secret", VaultMount.builder().type("kv").options(Collections.singletonMap("version", "1")).build());

		vaultTemplate.write("secret/" + VaultConfigRadiusTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));
	}

	private static void configureRadiusAuth(VaultTemplate vaultTemplate) {
		vaultTemplate.opsForSys().authMount("radius", VaultMount.create("radius"));

		String rules = """
				path "*" {
					capabilities = ["read", "list"]
				}
				""";
		vaultTemplate.write("sys/policy/testpolicy", Collections.singletonMap("rules", rules));

		// Configure RADIUS auth to point to the FreeRADIUS container by network alias
		vaultTemplate.write("auth/radius/config", Map.of("host", RADIUS_ALIAS, "secret", RADIUS_SECRET));

		vaultTemplate.write("auth/radius/users/" + RADIUS_USERNAME, Map.of("policies", "testpolicy"));
	}

	@Test
	void contextLoads() {
		this.contextRunner.run(ctx -> {
			TestApplication application = ctx.getBean(TestApplication.class);
			assertThat(application.configValue).isEqualTo("foo");
		});
	}

	@SpringBootApplication
	public static class TestApplication {

		@Value("${vault.value}")
		String configValue;

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
