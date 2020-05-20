/*
 * Copyright 2018-2020 the original author or authors.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test incorporating loading secrets using
 * {@code spring.cloud.vault.kv.profiles}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = VaultPropertySourceLocatorProfilesIntegrationTests.TestApplication.class,
		properties = { "spring.application.name=my-profiles-app",
				"spring.cloud.vault.kv.profiles=hello, world",
				"spring.cloud.vault.kv.default-context=" })
@ActiveProfiles({ "other" })
public class VaultPropertySourceLocatorProfilesIntegrationTests
		extends IntegrationTestSupport {

	@Autowired
	Environment environment;

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		vaultRule.prepare().getVaultOperations().write("secret/my-profiles-app/hello",
				Collections.singletonMap("vault.hello", "true"));

		vaultRule.prepare().getVaultOperations().write("secret/my-profiles-app/world",
				Collections.singletonMap("vault.world", "true"));

		vaultRule.prepare().getVaultOperations().write("secret/my-profiles-app/other",
				Collections.singletonMap("vault.other", "true"));
	}

	@Test
	public void shouldContainValuesFromKvProfiles() {
		assertThat(this.environment.getRequiredProperty("vault.hello")).isEqualTo("true");
		assertThat(this.environment.getRequiredProperty("vault.world")).isEqualTo("true");
	}

	@Test
	public void shouldNotContainVaulesFromSpringProfiles() {
		assertThat(this.environment.getProperty("vault.other")).isNull();
	}

	@SpringBootApplication
	public static class TestApplication {

	}

}
