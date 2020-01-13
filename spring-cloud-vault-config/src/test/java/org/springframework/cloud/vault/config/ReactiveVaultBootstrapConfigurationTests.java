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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.core.ReactiveVaultTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultReactiveBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class ReactiveVaultBootstrapConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(VaultBootstrapConfiguration.class,
					VaultReactiveBootstrapConfiguration.class));

	@Test
	public void shouldConfigureWithoutAuthentication() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.generic.enabled=false",
				"spring.cloud.vault.authentication=NONE").run(context -> {

					assertThat(context).doesNotHaveBean(SessionManager.class);
					assertThat(context).doesNotHaveBean(ClientAuthentication.class);
					assertThat(context).doesNotHaveBean(VaultTokenSupplier.class);
					assertThat(context).doesNotHaveBean(ReactiveSessionManager.class);
					assertThat(context).hasSingleBean(ReactiveVaultTemplate.class);
				});
	}

}
