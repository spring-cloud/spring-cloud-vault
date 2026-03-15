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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration test using config infrastructure with UserPass authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Issam El-atif
 */
class VaultConfigUserPassTests {

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigUserPassTests.class)
		.withAuthentication(VaultProperties.AuthenticationMethod.USERPASS)
		.withConfiguration(VaultConfigUserPassTests.TestApplication.class)
		.withProperties("spring.cloud.vault.userpass.username", "testuser")
		.withProperties("spring.cloud.vault.userpass.password", "testpass")
		.withSettings(VaultTestContextRunner.TestSettings::bootstrap);

	@BeforeAll
	static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.8.0")));

		VaultProperties vaultProperties = Settings.createVaultProperties();

		if (!vaultRule.prepare().hasAuth(vaultProperties.getUserpass().getPath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getUserpass().getPath());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = """
				{ "name": "testpolicy",
					"path": {
						"*": {  "policy": "read" }
					}
				}""";

		vaultOperations.write("sys/policy/testpolicy", Collections.singletonMap("rules", rules));

		vaultOperations.write("secret/" + VaultConfigUserPassTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		Map<String, String> userConfig = new HashMap<>();
		userConfig.put("password", "testpass");
		userConfig.put("policies", "testpolicy");

		vaultOperations.write("auth/userpass/users/testuser", userConfig);

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
