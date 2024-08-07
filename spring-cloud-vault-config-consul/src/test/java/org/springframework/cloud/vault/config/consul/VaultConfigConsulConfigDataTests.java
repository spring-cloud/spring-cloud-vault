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

package org.springframework.cloud.vault.config.consul;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for {@link VaultConfigConsulConfigDataTests}.
 *
 * @author Mark Paluch
 */
public class VaultConfigConsulConfigDataTests extends IntegrationTestSupport {

	private static final String POLICY = "key \"\" { policy = \"read\" }";

	private ConfigurableApplicationContext context;

	@Before
	public void before() {

		assumeTrue(SetupConsul.isConsulAvailable());

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		if (!prepare().hasSecretBackend("consul")) {
			prepare().mountSecret("consul");
		}

		SetupConsul.setupConsul(vaultOperations, "consul");

		Map<String, Object> role = new LinkedHashMap<>();
		role.put("policy", Base64.getEncoder().encodeToString(POLICY.getBytes()));
		role.put("ttl", "3s");
		role.put("max_ttl", "3s");
		vaultOperations.write(String.format("%s/roles/%s", "consul", "short-readonly"), role);

		this.vaultRule.prepare()
			.getVaultOperations()
			.write("secret/VaultConfigConsulConfigDataTests", Collections.singletonMap("default-key", "default"));

		SpringApplication application = new SpringApplication(VaultConfigConsulConfigDataTests.Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);

		this.context = application.run("--spring.application.name=VaultConfigConsulConfigDataTests",
				"--spring.config.import=vault:", "--spring.cloud.vault.kv.enabled=false",
				"--spring.cloud.vault.config.lifecycle.min-renewal=2s", "--spring.cloud.vault.consul.enabled=true",
				"--spring.cloud.vault.consul.role=short-readonly",
				"--spring.cloud.vault.token=" + Settings.token().getToken());
	}

	@Test
	public void shouldApplyConfigurer() throws InterruptedException {

		Config config = this.context.getBean(Config.class);

		assertThat(config.events).isEmpty();
		assertThat(this.context.getEnvironment().getProperty("spring.cloud.consul.config.acl-token")).isNotNull();

		Thread.sleep(5_000);
		assertThat(config.events).isNotEmpty();
	}

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class Config {

		final BlockingQueue<ConsulBackendMetadata.RebindConsulEvent> events = new LinkedBlockingQueue<>();

		@EventListener
		public void onRebind(ConsulBackendMetadata.RebindConsulEvent rebindConsulEvent) {
			this.events.add(rebindConsulEvent);
		}

	}

}
