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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link VaultHealthIndicatorAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Rastislav Zlacky
 */
class VaultHealthIndicatorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(VaultHealthIndicatorAutoConfiguration.class));

	@Test
	void shouldNotConfigureHealthIndicatorWithoutVaultOperations() {

		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean("vaultHealthIndicator").doesNotHaveBean("vaultReactiveHealthIndicator");
		});
	}

	@Test
	void shouldConfigureHealthIndicator() {

		this.contextRunner.withUserConfiguration(ImperativeConfiguration.class).run(context -> {
			assertThat(context).hasBean("vaultHealthIndicator");
		});
	}

	@Test
	void shouldConfigureReactiveHealthIndicator() {

		this.contextRunner.withUserConfiguration(ReactiveConfiguration.class).run(context -> {
			assertThat(context).hasBean("vaultReactiveHealthIndicator");
		});

	}

	@Test
	void shouldConfigureSingleHealthIndicator() {

		this.contextRunner.withUserConfiguration(ImperativeConfiguration.class, ReactiveConfiguration.class)
			.run(context -> {
				assertThat(context).hasBean("vaultHealthIndicator")
					.hasSingleBean(VaultReactiveHealthIndicator.class)
					.doesNotHaveBean(VaultHealthIndicator.class);
			});
	}

	static class ImperativeConfiguration {

		@Bean
		VaultOperations vaultOperations() {
			return mock(VaultOperations.class);
		}

	}

	static class ReactiveConfiguration {

		@Bean
		ReactiveVaultOperations reactiveVaultOperations() {
			return mock(ReactiveVaultOperations.class);
		}

	}

}
