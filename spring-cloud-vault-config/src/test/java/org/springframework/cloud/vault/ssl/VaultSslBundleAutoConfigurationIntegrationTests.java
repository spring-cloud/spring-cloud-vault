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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.vault.config.VaultConfigAppRoleTests;
import org.springframework.cloud.vault.config.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.certificate.ManagedCertificate;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@Test
	void shouldRegisterManagedCertificate() {

		this.contextRunner.withConfiguration(TestRegistrarApplication.class).run(it -> {
			TestRegistrarApplication app = it.getBean(TestRegistrarApplication.class);
			CertificateBundle bundle = app.bundle.get(5, TimeUnit.SECONDS);
			assertThat(bundle.getX509Certificate().getSubjectX500Principal().getName()).contains("www2.example.com");
		});
	}

	@Test
	void shouldFailStartupWhenCertificateIssuanceFails() {

		VaultTestContextRunner runner = this.contextRunner
			.withProperties("spring.cloud.vault.ssl.bundle.some-bundle.role-name", "testrole")
			.withProperties("spring.cloud.vault.ssl.bundle.some-bundle.common-name", "www.example.org")
			.withConfiguration(TestApplication.class);

		assertThatThrownBy(() -> {
			runner.run(context -> {
			});
		}).hasStackTraceContaining("Cannot obtain SSL bundle 'some-bundle'")
			.hasStackTraceContaining("www.example.org")
			.hasStackTraceContaining("not allowed by this role");
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(VaultConfigAppRoleTests.TestApplication.class, args);
		}

	}

	@SpringBootApplication
	public static class TestRegistrarApplication {

		final CompletableFuture<CertificateBundle> bundle = new CompletableFuture<>();

		public static void main(String[] args) {
			SpringApplication.run(VaultConfigAppRoleTests.TestApplication.class, args);
		}

		@Bean
		ManagedCertificate myBundle() {
			return ManagedCertificate.issue("server", "testrole", VaultCertificateRequest.create("www2.example.com"),
					this.bundle::complete, this.bundle::completeExceptionally);
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
