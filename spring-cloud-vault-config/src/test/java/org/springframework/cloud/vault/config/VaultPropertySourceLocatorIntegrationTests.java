/*
 * Copyright 2018-2021 the original author or authors.
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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test incorporating loading secrets using
 * {@code spring.cloud.vault.applicationName} and active profiles
 *
 * @author Ryan Hoegg
 * @author Mark Paluch
 */

@SpringBootTest(classes = VaultPropertySourceLocatorIntegrationTests.TestApplication.class,
		properties = { "spring.cloud.vault.kv.application-name=neuromancer,icebreaker",
				"spring.cloud.bootstrap.enabled=false", "spring.config.import=vault://" })
@ActiveProfiles({ "integrationtest" })
public class VaultPropertySourceLocatorIntegrationTests extends IntegrationTestSupport {

	@Value("${vault.value}")
	String configValue;

	@Value("${icebreaker.value}")
	String additionalValue;

	@BeforeAll
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/wintermute", Collections.singletonMap("vault.value", "wintermute"));
		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/wintermute/integrationtest",
					Collections.singletonMap("vault.value", "integrationtest wintermute"));

		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/neuromancer", Collections.singletonMap("vault.value", "neuromancer"));
		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/neuromancer/integrationtest",
					Collections.singletonMap("vault.value", "integrationtest neuromancer"));

		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/icebreaker", Collections.singletonMap("icebreaker.value", "icebreaker"));
		vaultRule.prepare()
			.getVaultOperations()
			.write("secret/icebreaker/integrationtest",
					Collections.singletonMap("icebreaker.value", "integrationtest icebreaker"));
	}

	@Test
	public void getsSecretFromVaultUsingVaultApplicationName() {
		assertThat(this.configValue).isEqualTo("integrationtest neuromancer");
	}

	@Test
	public void getsSecretFromVaultUsingAdditionalContext() {
		assertThat(this.additionalValue).isEqualTo("integrationtest icebreaker");
	}

	@SpringBootApplication
	public static class TestApplication {

	}

}
