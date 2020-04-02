/*
 * Copyright 2020 the original author or authors.
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

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.TestRestTemplateFactory;
import org.springframework.vault.client.ClientHttpConnectorFactory;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.ClientOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link VaultReactiveHealthIndicator}.
 *
 * @author Mark Paluch
 */
public class VaultReactiveHealthIndicatorIntegrationTests extends IntegrationTestSupport {

	@Test
	public void shouldReturnHealthState() {

		ReactiveVaultTemplate vaultTemplate = new ReactiveVaultTemplate(
				TestRestTemplateFactory.TEST_VAULT_ENDPOINT, ClientHttpConnectorFactory
				.create(new ClientOptions(), Settings
						.createSslConfiguration()), () -> Mono.just(Settings.token()));

		VaultReactiveHealthIndicator healthIndicator = new VaultReactiveHealthIndicator(vaultTemplate);

		healthIndicator.doHealthCheck(Health.up()).as(StepVerifier::create)
				.consumeNextWith(actual -> {
					assertThat(actual.getStatus()).isEqualTo(Status.UP);
				}).verifyComplete();
	}
}
