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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
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
 * Integration test using config infrastructure with Okta authentication.
 *
 * <p>
 * This test uses Testcontainers to run Vault and MockWebServer to simulate the Okta
 * authentication API. Testcontainers is required here because Vault's Okta auth method
 * makes outbound HTTPS calls to the Okta API (e.g., {@code https://mock.localhost:8443}).
 * Without Testcontainers, this would require modifying {@code /etc/hosts} to resolve
 * {@code mock.localhost} to {@code 127.0.0.1}, which is not feasible in automated tests.
 * Testcontainers solves this DNS limitation by using
 * {@code withExtraHost("mock.localhost", "host-gateway")}, which adds a host entry inside
 * the container that maps {@code mock.localhost} to the host machine, allowing Vault to
 * reach the MockWebServer running on the host.
 *
 * @author Issam El-atif
 */
@Testcontainers
class VaultConfigOktaTests {

	private static final String VAULT_TOKEN = "00000000-0000-0000-0000-000000000000";

	private static final int MOCK_SERVER_PORT = 8443;

	private static final String OKTA_USERNAME = "testuser";

	private static final String OKTA_PASSWORD = "testpass";

	private static final String OKTA_TOTP = "totp";

	private static MockWebServer oktaServer;

	private static final HeldCertificate caCertificate;

	private static final HeldCertificate serverCertificate;

	private static final Path caCertFile;

	@Container
	static VaultContainer<?> vaultContainer;

	static {
		try {
			caCertificate = new HeldCertificate.Builder().certificateAuthority(0).build();

			serverCertificate = new HeldCertificate.Builder().addSubjectAlternativeName("mock.localhost")
				.signedBy(caCertificate)
				.build();

			caCertFile = Files.createTempFile("ca-cert", ".pem");
			Files.writeString(caCertFile, caCertificate.certificatePem());

			vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3").withVaultToken(VAULT_TOKEN)
				.withExtraHost("mock.localhost", "host-gateway")
				.withAccessToHost(true)
				.withFileSystemBind(caCertFile.toString(), "/etc/ssl/certs/mock-ca.pem", BindMode.READ_ONLY)
				.withEnv("SSL_CERT_FILE", "/etc/ssl/certs/mock-ca.pem");
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to create certificates", ex);
		}
	}

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigOktaTests.class)
		.withAuthentication(VaultProperties.AuthenticationMethod.OKTA)
		.withConfiguration(TestApplication.class)
		.withProperties("spring.cloud.vault.uri", vaultContainer.getHttpHostAddress())
		.withProperties("spring.cloud.vault.okta.username", OKTA_USERNAME)
		.withProperties("spring.cloud.vault.okta.password", OKTA_PASSWORD)
		.withProperties("spring.cloud.vault.okta.totp", OKTA_TOTP)
		.withSettings(VaultTestContextRunner.TestSettings::bootstrap);

	@BeforeAll
	static void beforeClass() throws Exception {
		oktaServer = createMockOktaServer();
		oktaServer.start(MOCK_SERVER_PORT);

		VaultTemplate vaultTemplate = createVaultTemplate();
		configureVaultSecrets(vaultTemplate);
		configureOktaAuth(vaultTemplate);
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

		vaultTemplate.write("secret/" + VaultConfigOktaTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));
	}

	private static void configureOktaAuth(VaultTemplate vaultTemplate) {
		vaultTemplate.opsForSys().authMount("okta", VaultMount.create("okta"));

		String rules = """
				path "*" {
					capabilities = ["read", "list"]
				}
				""";
		vaultTemplate.write("sys/policy/testpolicy", Collections.singletonMap("rules", rules));

		// Vault constructs Okta URL as: https://{org_name}.{base_url}/api/v1/authn
		vaultTemplate.write("auth/okta/config",
				Map.of("org_name", "mock", "base_url", "localhost:" + MOCK_SERVER_PORT));

		vaultTemplate.write("auth/okta/users/" + OKTA_USERNAME, Map.of("policies", "testpolicy"));
	}

	private static MockWebServer createMockOktaServer() {
		MockWebServer server = new MockWebServer();

		HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
			.heldCertificate(serverCertificate, caCertificate.certificate())
			.build();

		server.useHttps(serverCertificates.sslSocketFactory(), false);
		server.setDispatcher(new OktaDispatcher());

		return server;
	}

	@AfterAll
	static void afterClass() throws IOException {
		if (oktaServer != null) {
			oktaServer.shutdown();
		}
		if (caCertFile != null) {
			Files.deleteIfExists(caCertFile);
		}
	}

	@Test
	void contextLoads() {
		this.contextRunner.run(ctx -> {
			TestApplication application = ctx.getBean(TestApplication.class);
			assertThat(application.configValue).isEqualTo("foo");
		});
	}

	static class OktaDispatcher extends Dispatcher {

		@NotNull
		@Override
		public MockResponse dispatch(RecordedRequest request) {
			if ("/api/v1/authn".equals(request.getPath()) && "POST".equals(request.getMethod())) {
				String requestBody = request.getBody().readUtf8();

				if (requestBody.contains("\"username\":\"" + OKTA_USERNAME + "\"")
						&& requestBody.contains("\"password\":\"" + OKTA_PASSWORD + "\"")) {

					return new MockResponse().setResponseCode(200)
						.setHeader("Content-Type", "application/json")
						.setBody("""
								{
									"status": "SUCCESS",
									"sessionToken": "mock-session-token"
								}
								""");
				}

				return new MockResponse().setResponseCode(401).setHeader("Content-Type", "application/json").setBody("""
						{
							"errorCode": "E0000004",
							"errorSummary": "Authentication failed"
						}
						""");
			}

			return new MockResponse().setResponseCode(404);
		}

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
