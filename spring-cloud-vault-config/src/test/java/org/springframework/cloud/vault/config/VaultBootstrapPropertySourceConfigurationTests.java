/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.vault.config;

import java.time.Duration;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link VaultBootstrapPropertySourceConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultBootstrapPropertySourceConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations
					.of(VaultBootstrapPropertySourceConfiguration.class));

	@Test
	public void shouldConfigureExpiryTimeouts() {

		this.contextRunner.withUserConfiguration(MockConfiguration.class)
				.withPropertyValues("spring.cloud.vault.generic.enabled=false",
						"spring.cloud.vault.config.lifecycle.expiry-threshold=5m",
						"spring.cloud.vault.config.lifecycle.min-renewal=6m")
				.run(context -> {

					SecretLeaseContainer container = context
							.getBean(SecretLeaseContainer.class);
					assertThat(container.getExpiryThreshold())
							.isEqualTo(Duration.ofMinutes(5));
					assertThat(container.getMinRenewal())
							.isEqualTo(Duration.ofMinutes(6));
				});
	}

	@EnableConfigurationProperties(VaultProperties.class)
	private static class MockConfiguration {

		@Bean
		VaultOperations vaultOperations() {
			return mock(VaultOperations.class);
		}

		@Bean
		VaultBootstrapConfiguration.TaskSchedulerWrapper taskSchedulerWrapper() {
			return new VaultBootstrapConfiguration.TaskSchedulerWrapper(
					mock(ThreadPoolTaskScheduler.class));
		}

	}

}
