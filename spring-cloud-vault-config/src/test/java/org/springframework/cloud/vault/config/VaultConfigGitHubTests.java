/*
 * Copyright 2017-2021 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.internal.creation.MockSettingsImpl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Policy;
import org.springframework.vault.support.Policy.BuiltinCapabilities;
import org.springframework.vault.support.Policy.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * Integration test using config infrastructure with GitHub authentication.
 *
 * @author Issam El-atif
 */
public class VaultConfigGitHubTests {

	private ConfigurableApplicationContext context;

	@BeforeClass
	public static void beforeClass() throws IOException {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasAuth("github")) {
			vaultRule.prepare().mountAuth("github");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Policy policy = Policy.of(Rule.builder().path("*").capabilities(BuiltinCapabilities.READ).build());

		vaultOperations.opsForSys().createOrUpdatePolicy("testpolicy", policy);

		vaultOperations.write("secret/" + VaultConfigGitHubTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				assert recordedRequest.getPath() != null;
				return switch (recordedRequest.getPath()) {
					case "/user" -> new MockResponse().setBody("{\"login\":\"vault\",\"id\":1}");
					case "/user/orgs?per_page=100" ->
						new MockResponse().setBody("[{\"login\":\"vault-org\",\"id\":1}]");
					default -> new MockResponse();
				};
			}
		});
		server.start();

		Map<String, Object> githubConfig = new HashMap<>();
		githubConfig.put("base_url", "http://localhost:" + server.getPort());
		githubConfig.put("organization", "vault-org");
		githubConfig.put("organization_id", 1);
		githubConfig.put("token_policies", "testpolicy");

		vaultOperations.write("auth/github/config", githubConfig);
	}

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void authenticateWithPropertyToken() {
		SpringApplication app = new SpringApplication(TestConfig.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		this.context = app.run("--spring.cloud.vault.authentication=github",
				"--spring.cloud.vault.application-name=VaultConfigGitHubTests", "--spring.cloud.bootstrap.enabled=true",
				"--spring.cloud.vault.github.token=gho_00000000-0000-0000-0000-000000000000");

		assertThat(this.context.getEnvironment().getProperty("vault.value")).isEqualTo("foo");

	}

	@Test
	public void authenticateWithGithubCliToken() {
		// Mock Process for gh cli calls
		MockedConstruction<ProcessBuilder> processBuilderMockedConstruction = mockConstruction(ProcessBuilder.class,
				context -> new MockSettingsImpl<>().defaultAnswer(RETURNS_MOCKS), (builderMock, context) -> {
					Process processMock = mock(Process.class);
					when(builderMock.start()).thenReturn(processMock);
					when(processMock.waitFor()).thenReturn(0);
					when(processMock.getInputStream())
						.thenReturn(new ByteArrayInputStream("gho_00000000-0000-0000-0000-000000000000".getBytes()));
				});

		SpringApplication app = new SpringApplication(TestConfig.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		this.context = app.run("--spring.cloud.vault.authentication=github",
				"--spring.cloud.vault.application-name=VaultConfigGitHubTests",
				"--spring.cloud.bootstrap.enabled=true");

		assertThat(this.context.getEnvironment().getProperty("vault.value")).isEqualTo("foo");

		processBuilderMockedConstruction.close();
	}

	@Test
	public void authenticationFailWhenNoTokenProvided() {
		MockedConstruction<ProcessBuilder> processBuilderMockedConstruction = mockConstruction(ProcessBuilder.class,
				context -> new MockSettingsImpl<>().defaultAnswer(RETURNS_MOCKS), (builderMock, context) -> {
					Process processMock = mock(Process.class);
					when(builderMock.start()).thenReturn(processMock);
					// GitHub cli not installed return exitcode=1
					when(processMock.waitFor()).thenReturn(1);
				});

		SpringApplication app = new SpringApplication(TestConfig.class);
		app.setWebApplicationType(WebApplicationType.NONE);

		assertThatThrownBy(() -> app.run("--spring.cloud.vault.authentication=github",
				"--spring.cloud.vault.application-name=VaultConfigGitHubTests",
				"--spring.cloud.bootstrap.enabled=true"))
			.getRootCause()
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Cannot create authentication mechanism for GITHUB. This method requires a Token "
					+ "supplied by spring.cloud.vault.token or GitHub CLI.");

		processBuilderMockedConstruction.close();
	}

	@TestConfiguration
	public static class TestConfig {

	}

}
