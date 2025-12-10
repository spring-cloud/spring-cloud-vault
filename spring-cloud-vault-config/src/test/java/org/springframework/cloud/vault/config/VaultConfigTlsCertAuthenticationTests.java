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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.vault.util.Settings.findWorkDir;

/**
 * Integration test using config infrastructure with TLS certificate authentication. In
 * case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 * @author Issam El-atif
 */
public class VaultConfigTlsCertAuthenticationTests {

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigTlsCertAuthenticationTests.class)
		.withAuthentication(VaultProperties.AuthenticationMethod.CERT)
		.withConfiguration(TestConfig.class)
		.withSettings(s -> s.bootstrap());

	private static VaultOperations vaultOperations;

	private static final String certificate = Files.contentOf(new File(findWorkDir(), "ca/certs/client.cert.pem"),
			StandardCharsets.US_ASCII);

	@BeforeAll
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		VaultProperties vaultProperties = Settings.createVaultProperties();

		if (!vaultRule.prepare().hasAuth(vaultProperties.getSsl().getCertAuthPath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getSsl().getCertAuthPath());
		}

		vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = "{ \"name\": \"testpolicy\",\n" //
				+ "  \"path\": {\n" //
				+ "    \"*\": {  \"policy\": \"read\" }\n" //
				+ "  }\n" //
				+ "}";

		vaultOperations.write("sys/policy/testpolicy", Collections.singletonMap("rules", rules));

		vaultOperations.write("secret/" + VaultConfigTlsCertAuthenticationTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		Map<String, String> role = new HashMap<>();
		role.put("certificate", certificate);
		role.put("policies", "testpolicy");

		vaultOperations.write("auth/cert/certs/my-role", role);
	}

	@AfterEach
	void cleanup() {
		vaultOperations.delete("auth/cert/certs/another-role");
	}

	@Test
	void authenticateUsingTlsCertificate() {
		this.contextRunner
			.run(context -> assertThat(context.getEnvironment().getProperty("vault.value")).isEqualTo("foo"));
	}

	@Test
	void authenticateUsingNamedCertificateRole() {
		Map<String, String> anotherRole = new HashMap<>();
		anotherRole.put("certificate", certificate);
		anotherRole.put("policies", "another-policy");
		vaultOperations.write("auth/cert/certs/another-role", anotherRole);

		this.contextRunner.withProperties("spring.cloud.vault.ssl.role", "my-role").run(context -> {
			VaultTemplate template = context.getBean(VaultTemplate.class);
			VaultResponse tokenLookup = template.read("auth/token/lookup-self");
			assertThat(tokenLookup.getRequiredData()).containsEntry("policies", List.of("default", "testpolicy"));
			assertThat(context.getEnvironment().getProperty("vault.value")).isEqualTo("foo");
		});
	}

	@Test
	void authenticationFailsWhenMultipleCertsAndNoNamedRole() {
		Map<String, String> anotherRole = new HashMap<>();
		anotherRole.put("certificate", certificate);
		vaultOperations.write("auth/cert/certs/another-role", anotherRole);

		this.contextRunner.run(context -> {
			VaultTemplate template = context.getBean(VaultTemplate.class);
			VaultResponse tokenLookup = template.read("auth/token/lookup-self");
			assertThat(tokenLookup.getRequiredData()).containsEntry("policies", Collections.singletonList("default"));
			assertThat(context.getEnvironment().getProperty("vault.value")).isNull();
		});
	}

	@TestConfiguration
	public static class TestConfig {

	}

}
