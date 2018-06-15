/*
 * Copyright 2017-2018 the original author or authors.
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

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.AuthenticationSteps;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link VaultReactiveBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultReactiveBootstrapConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void shouldConfigureTemplate() {

		load(AuthenticationFactoryConfiguration.class,
				"spring.cloud.vault.config.lifecycle.enabled=false");

		assertThat(context.getBean(ReactiveVaultOperations.class)).isNotNull();
		assertThat(context.getBean(AuthenticationStepsFactory.class)).isNotNull();
		assertThat(context.getBean(SessionManager.class)).isNotNull()
				.isNotInstanceOf(LifecycleAwareSessionManager.class)
				.isNotInstanceOf(SimpleSessionManager.class);
		assertThat(context.getBeanNamesForType(WebClient.class)).isEmpty();
	}

	@Test
	public void shouldConfigureTemplateWithTokenSupplier() {

		load(TokenSupplierConfiguration.class,
				"spring.cloud.vault.config.lifecycle.enabled=false");

		assertThat(context.getBean(ReactiveVaultOperations.class)).isNotNull();
		assertThat(context.getBean(SessionManager.class)).isNotNull()
				.isNotInstanceOf(LifecycleAwareSessionManager.class)
				.isNotInstanceOf(SimpleSessionManager.class);
		assertThat(context.getBeanNamesForType(WebClient.class)).isEmpty();
	}

	@Test
	public void shouldNotConfigureReactiveSupport() {

		load(VaultBootstrapConfiguration.class,
				"spring.cloud.vault.reactive.enabled=false",
				"spring.cloud.vault.token=foo");

		assertThat(context.getBeanNamesForType(ReactiveVaultOperations.class)).isEmpty();
		assertThat(context.getBean(SessionManager.class)).isInstanceOf(
				LifecycleAwareSessionManager.class);
	}

	private void load(Class<?> config, String... environment) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		TestPropertyValues.of(environment).applyTo(ctx);

		ctx.register(config);
		ctx.register(VaultReactiveBootstrapConfiguration.class);
		ctx.refresh();

		this.context = ctx;
	}

	@Configuration
	static class AuthenticationFactoryConfiguration {

		@Bean
		AuthenticationStepsFactory authenticationStepsFactory() {
			return () -> AuthenticationSteps.just(VaultToken.of("foo"));
		}
	}

	@Configuration
	static class TokenSupplierConfiguration {

		@Bean
		VaultTokenSupplier vaultTokenSupplier() {
			return () -> Mono.just(VaultToken.of("foo"));
		}
	}
}
