/*
 * Copyright 2020-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultConfigDataLoader}.
 *
 * @author Mark Paluch
 */
public class VaultConfigDataLoaderIntegrationTests extends IntegrationTestSupport {

	private ConfigurableApplicationContext context;

	@Before
	public void before() {

		this.vaultRule.prepare().getVaultOperations().write("secret/my-config-loader",
				Collections.singletonMap("default-key", "default"));

		this.vaultRule.prepare().getVaultOperations().write("secret/my-config-loader/cloud",
				Collections.singletonMap("default-key", "cloud"));

		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("cloud");

		this.context = application.run("--spring.application.name=my-config-loader", "--spring.config.import=vault:",
				"--spring.cloud.vault.token=" + Settings.token().getToken());
	}

	@Test
	public void shouldConsiderProfiles() {
		assertThat(this.context.getEnvironment().getProperty("default-key")).isEqualTo("cloud");
	}

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	private static class Config {

	}

}
