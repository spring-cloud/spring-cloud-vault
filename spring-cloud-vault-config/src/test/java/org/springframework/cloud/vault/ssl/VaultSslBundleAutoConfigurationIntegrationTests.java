/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.cloud.vault.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.vault.config.VaultConfigAppRoleTests;
import org.springframework.cloud.vault.config.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Integration tests for {@link VaultSslBundleAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class VaultSslBundleAutoConfigurationIntegrationTests extends PkiIntegrationTestSupport {

	VaultTestContextRunner contextRunner = VaultTestContextRunner
		.of(VaultSslBundleAutoConfigurationIntegrationTests.class)
		.withAuthentication(AuthenticationMethod.TOKEN)
		.withProperties("spring.cloud.vault.token", Settings.token().getToken());

	@Test
	void shouldRegisterPropertiesSslBundle() {

		this.contextRunner.withProperties("spring.cloud.vault.ssl.bundle.some-bundle.role-name", "testrole")
			.withProperties("spring.cloud.vault.ssl.bundle.some-bundle.common-name", "www.example.com")
			.withConfiguration(TestApplication.class)
			.run(it -> {
				SslBundles sslBundles = it.getBean(SslBundles.class);
				assertThat(sslBundles.getBundleNames()).contains("some-bundle");
			});
	}

	@Test
	void shouldRegisterSslBundle() {

		this.contextRunner.withConfiguration(TestRegistrarApplication.class).run(it -> {
			SslBundles sslBundles = it.getBean(SslBundles.class);
			assertThat(sslBundles.getBundleNames()).contains("www.example.com");
		});
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(VaultConfigAppRoleTests.TestApplication.class, args);
		}

	}

	@SpringBootApplication
	public static class TestRegistrarApplication {

		public static void main(String[] args) {
			SpringApplication.run(VaultConfigAppRoleTests.TestApplication.class, args);
		}

		@Bean
		VaultSslBundleRegistrar registrar() {
			return registry -> {
				registry.register("www.example.com", bundleSpec -> {
					return bundleSpec.issueCertificate("testrole");

				});
			};
		}

	}

}
