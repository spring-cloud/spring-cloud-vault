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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.certificate.domain.RequestedCertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;

/**
 * Unit tests for {@link PropertiesVaultSslBundlesRegistrar}.
 *
 * @author Mark Paluch
 */
class PropertiesVaultSslBundlesRegistrarTests {

	ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PropertiesConfiguration.class));

	@Test
	void shouldNotRegisterEmptyBundles() {

		this.runner.run(ctx -> {
			DefaultVaultSslBundleRegistry registry = new DefaultVaultSslBundleRegistry();
			PropertiesVaultSslBundlesRegistrar registrar = ctx.getBean(PropertiesVaultSslBundlesRegistrar.class);
			registrar.register(registry);

			assertThat(registry.getManagedSslBundles()).isEmpty();
		});
	}

	@Test
	void shouldRegisterSimpleBundle() {

		this.runner.withPropertyValues("spring.cloud.vault.ssl.bundle.localhost.role-name=my-role").run(ctx -> {
			DefaultVaultSslBundleRegistry registry = new DefaultVaultSslBundleRegistry();
			PropertiesVaultSslBundlesRegistrar registrar = ctx.getBean(PropertiesVaultSslBundlesRegistrar.class);
			registrar.register(registry);

			assertThat(registry.getManagedSslBundles()).hasSize(1);

			VaultManagedSslBundle request = registry.getManagedSslBundles().iterator().next();
			RequestedCertificateBundle bundle = (RequestedCertificateBundle) request.requestedCertificate();

			assertThat(request.getName()).isEqualTo("localhost");
			assertThat(bundle.getRole()).isEqualTo("my-role");
			assertThat(request.sslProtocol()).isEqualTo(SslBundle.DEFAULT_PROTOCOL);
			assertThat(bundle.getRequest().getCommonName()).isEqualTo("localhost");
			assertThat(bundle.getRequest().getTtl()).isNull();
		});
	}

	@Test
	void shouldRegisterFullBundle() {

		this.runner
			.withPropertyValues("spring.cloud.vault.ssl.bundle.localhost.role-name=my-role",
					"spring.cloud.vault.ssl.bundle.localhost.common-name=www.example.com",
					"spring.cloud.vault.ssl.bundle.localhost.ttl=24m",
					"spring.cloud.vault.ssl.bundle.localhost.alt-names=foo,bar",
					"spring.cloud.vault.ssl.bundle.localhost.ip-sans=127.0.0.1",
					"spring.cloud.vault.ssl.bundle.localhost.uri-sans=foo@bar",
					"spring.cloud.vault.ssl.bundle.localhost.other-sans=something,else",
					"spring.cloud.vault.ssl.bundle.localhost.protocol=TLSv1",
					"spring.cloud.vault.ssl.bundle.localhost.options.ciphers=AES,FOO",
					"spring.cloud.vault.ssl.bundle.localhost.options.enabled-protocols=TLSv1.2,TLSv1.3")
			.run(ctx -> {
				DefaultVaultSslBundleRegistry registry = new DefaultVaultSslBundleRegistry();
				PropertiesVaultSslBundlesRegistrar registrar = ctx.getBean(PropertiesVaultSslBundlesRegistrar.class);
				registrar.register(registry);

				assertThat(registry.getManagedSslBundles()).hasSize(1);

				VaultManagedSslBundle request = registry.getManagedSslBundles().iterator().next();
				RequestedCertificateBundle bundle = (RequestedCertificateBundle) request.requestedCertificate();

				assertThat(bundle.getName()).isEqualTo("localhost");
				assertThat(bundle.getRole()).isEqualTo("my-role");
				assertThat(request.sslProtocol()).isEqualTo("TLSv1");

				VaultCertificateRequest cert = bundle.getRequest();
				assertThat(cert.getCommonName()).isEqualTo("www.example.com");
				assertThat(cert.getTtl()).isEqualTo(Duration.ofMinutes(24));
				assertThat(cert.getAltNames()).containsExactly("foo", "bar");
				assertThat(cert.getIpSubjectAltNames()).containsExactly("127.0.0.1");
				assertThat(cert.getUriSubjectAltNames()).containsExactly("foo@bar");
				assertThat(cert.getOtherSans()).containsExactly("something", "else");

				assertThat(request.sslOptions().getCiphers()).contains("AES", "FOO");
				assertThat(request.sslOptions().getEnabledProtocols()).contains("TLSv1.2", "TLSv1.3");
			});
	}

	@Configuration
	@EnableConfigurationProperties({ VaultProperties.class, VaultSslBundlesProperties.class })
	static class PropertiesConfiguration {

		@Bean
		PropertiesVaultSslBundlesRegistrar propertiesVaultSslBundlesRegistrar(VaultSslBundlesProperties bundles) {
			return new PropertiesVaultSslBundlesRegistrar(bundles);
		}

	}

}
