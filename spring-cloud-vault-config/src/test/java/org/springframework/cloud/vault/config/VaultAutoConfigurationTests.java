/*
 * Copyright 2017-present the original author or authors.
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
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.RestClientCustomizer;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultClientCustomizer;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link VaultAutoConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(VaultAutoConfiguration.class));

	@Test
	public void shouldApplyCustomizers() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.session.lifecycle.enabled=false")
			.withPropertyValues("spring.cloud.vault.config.lifecycle.enabled=false")
			.withBean(TokenAuthentication.class, "token")
			.withBean(RestClientCustomizer.class, () -> {
				return builder -> {
					builder.requestFactory((uri, method) -> {
						throw new IllegalStateException("customized");
					});
				};
			})
			.run(context -> {
				VaultClient client = context.getBean(VaultClient.class);
				assertThatIllegalStateException().isThrownBy(() -> client.get().retrieve().requiredBody())
					.withMessage("customized");
			});

		this.contextRunner.withPropertyValues("spring.cloud.vault.session.lifecycle.enabled=false")
			.withPropertyValues("spring.cloud.vault.config.lifecycle.enabled=false")
			.withBean(TokenAuthentication.class, "token")
			.withBean(VaultClientCustomizer.class, () -> {
				return builder -> {
					builder.requestFactory((uri, method) -> {
						throw new IllegalStateException("customized");
					});
				};
			})
			.run(context -> {
				VaultClient client = context.getBean(VaultClient.class);
				assertThatIllegalStateException().isThrownBy(() -> client.get().retrieve().requiredBody())
					.withMessage("customized");
			});
	}

}
