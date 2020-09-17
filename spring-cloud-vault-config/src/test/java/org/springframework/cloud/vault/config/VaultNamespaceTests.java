/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.TestRestTemplateFactory;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.ClientHttpConnectorFactory;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.core.VaultSysOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.Policy;
import org.springframework.vault.support.VaultMount;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultTokenRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Vault's namespace feature.
 */
@RunWith(SpringRunner.class)
public class VaultNamespaceTests {

	@ClassRule
	public static VaultRule vaultRule = new VaultRule();

	static final Policy POLICY = Policy
			.of(Policy.Rule.builder().path("/*").capabilities(Policy.BuiltinCapabilities.READ,
					Policy.BuiltinCapabilities.CREATE, Policy.BuiltinCapabilities.UPDATE).build());

	RestTemplateBuilder maketingRestTemplate;

	WebClientBuilder marketingWebClientBuilder = WebClientBuilder.builder()
			.httpConnector(ClientHttpConnectorFactory.create(new ClientOptions(), Settings.createSslConfiguration()))
			.endpoint(TestRestTemplateFactory.TEST_VAULT_ENDPOINT)
			.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, "marketing");

	String marketingToken;

	@Before
	public void before() {
		Assume.assumeTrue("Namespaces require enterprise version",
				this.vaultRule.prepare().getVersion().isEnterprise());

		List<String> namespaces = new ArrayList<>(Arrays.asList("dev/", "marketing/"));
		List<String> list = this.vaultRule.prepare().getVaultOperations().list("sys/namespaces");
		namespaces.removeAll(list);

		for (String namespace : namespaces) {
			this.vaultRule.prepare().getVaultOperations().write("sys/namespaces/" + namespace.replaceAll("/", ""));
		}

		this.maketingRestTemplate = RestTemplateBuilder.builder()
				.requestFactory(
						ClientHttpRequestFactoryFactory.create(new ClientOptions(), Settings.createSslConfiguration()))
				.endpoint(TestRestTemplateFactory.TEST_VAULT_ENDPOINT)
				.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, "marketing");

		VaultTemplate marketing = new VaultTemplate(this.maketingRestTemplate,
				new SimpleSessionManager(new TokenAuthentication(Settings.token())));

		mountKv(marketing, "marketing-secrets");
		marketing.opsForSys().createOrUpdatePolicy("relaxed", POLICY);
		this.marketingToken = marketing.opsForToken().create(VaultTokenRequest.builder().withPolicy("relaxed").build())
				.getToken().getToken();
	}

	private void mountKv(VaultTemplate template, String path) {

		VaultSysOperations vaultSysOperations = template.opsForSys();

		Map<String, VaultMount> mounts = vaultSysOperations.getMounts();

		if (!mounts.containsKey(path + "/")) {
			vaultSysOperations.mount(path,
					VaultMount.builder().type("kv").options(Collections.singletonMap("version", "1")).build());
		}
	}

	@Test
	public void shouldReportHealth() {

		VaultTemplate marketing = new VaultTemplate(this.maketingRestTemplate,
				new SimpleSessionManager(new TokenAuthentication(this.marketingToken)));

		Health.Builder builder = Health.unknown();
		new VaultHealthIndicator(marketing).doHealthCheck(builder);

		assertThat(builder.build().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void shouldReportReactiveHealth() {

		ReactiveVaultTemplate reactiveMarketing = new ReactiveVaultTemplate(this.marketingWebClientBuilder,
				() -> Mono.just(VaultToken.of(this.marketingToken)));

		Health.Builder builder = Health.unknown();

		new VaultReactiveHealthIndicator(reactiveMarketing).doHealthCheck(builder).as(StepVerifier::create)
				.assertNext(actual -> assertThat(actual.getStatus()).isEqualTo(Status.UP)).verifyComplete();
	}

}
