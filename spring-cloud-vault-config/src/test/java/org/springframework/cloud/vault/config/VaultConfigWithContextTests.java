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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using config infrastructure with token authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
public class VaultConfigWithContextTests {

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigTlsCertAuthenticationTests.class)
		.withConfiguration(VaultConfigWithContextTests.TestApplication.class)
		.withApplicationName("testVaultApp")
		.withProfiles("my-profile")
		.withSettings(s -> s.bootstrap());

	@BeforeAll
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();
		vaultOperations.write("secret/testVaultApp/my-profile", Collections.singletonMap("vault.value", "hello"));
		vaultOperations.write("secret/testVaultApp", Collections.singletonMap("vault.value", "world"));
	}

	@Test
	public void contextLoads() {
		this.contextRunner.run(ctx -> {
			TestApplication application = ctx.getBean(TestApplication.class);
			assertThat(application.configValue).isEqualTo("hello");
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
