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

package org.springframework.cloud.vault.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.vault.authentication.AuthenticationSteps;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.CachingVaultTokenSupplier;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.WebClientFactory;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link VaultReactiveAutoConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(VaultReactiveAutoConfiguration.class));

	@Test
	public void shouldConfigureTemplate() {

		this.contextRunner.withUserConfiguration(AuthenticationFactoryConfiguration.class)
			.withPropertyValues("spring.cloud.vault.session.lifecycle.enabled=false")
			.run(context -> {

				assertThat(context).hasSingleBean(ReactiveVaultOperations.class);
				assertThat(context).hasSingleBean(AuthenticationStepsFactory.class);
				assertThat(context.getBean(SessionManager.class)).isNotNull()
					.isNotInstanceOf(LifecycleAwareSessionManager.class)
					.isNotInstanceOf(SimpleSessionManager.class);
				assertThat(context.getBeanNamesForType(WebClient.class)).isEmpty();
				assertThat(context).hasSingleBean(WebClientFactory.class);
			});
	}

	@Test
	public void shouldNotConfigureIfHttpClientIsMissing() {

		this.contextRunner.withUserConfiguration(AuthenticationFactoryConfiguration.class)
			.withClassLoader(new FilteredClassLoader("reactor.netty.http.client.HttpClient"))
			.run(context -> {

				assertThat(context).doesNotHaveBean(ReactiveVaultOperations.class);
			});
	}

	@Test
	public void shouldConfigureTemplateWithTokenSupplier() {

		this.contextRunner.withUserConfiguration(TokenSupplierConfiguration.class)
			.withPropertyValues("spring.cloud.vault.session.lifecycle.enabled=false")
			.run(context -> {

				assertThat(context).hasSingleBean(ReactiveVaultOperations.class);
				assertThat(context.getBean(SessionManager.class)).isNotNull()
					.isNotInstanceOf(LifecycleAwareSessionManager.class)
					.isNotInstanceOf(SimpleSessionManager.class);
				assertThat(context).doesNotHaveBean(WebClient.class);
			});
	}

	@Test
	public void shouldNotConfigureReactiveSupport() {

		this.contextRunner.withUserConfiguration(VaultAutoConfiguration.class)
			.withPropertyValues("spring.cloud.vault.reactive.enabled=false", "spring.cloud.vault.token=foo")
			.run(context -> {

				assertThat(context).doesNotHaveBean(ReactiveVaultTemplate.class)
					.doesNotHaveBean(ReactiveVaultOperations.class);
				assertThat(context.getBean(SessionManager.class)).isInstanceOf(LifecycleAwareSessionManager.class);
			});
	}

	@Test
	public void sessionManagerBridgeShouldNotCacheTokens() {

		this.contextRunner.withUserConfiguration(TokenSupplierConfiguration.class, CustomSessionManager.class)
			.run(context -> {

				SessionManager sessionManager = context.getBean(SessionManager.class);

				assertThat(sessionManager.getSessionToken().getToken()).isEqualTo("token-1");
				assertThat(sessionManager.getSessionToken().getToken()).isEqualTo("token-2");
			});
	}

	@Test
	public void shouldDisableSessionManagement() {

		this.contextRunner
			.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
					"spring.cloud.vault.session.lifecycle.enabled=false")
			.withBean("vaultTokenSupplier", VaultTokenSupplier.class, () -> Mono::empty)
			.withBean("taskSchedulerWrapper", VaultAutoConfiguration.TaskSchedulerWrapper.class,
					() -> new VaultAutoConfiguration.TaskSchedulerWrapper(new ThreadPoolTaskScheduler()))
			.run(context -> {

				ReactiveSessionManager bean = context.getBean(ReactiveSessionManager.class);
				assertThat(bean).isExactlyInstanceOf(CachingVaultTokenSupplier.class);
			});
	}

	@Test
	public void shouldConfigureSessionManagement() {

		this.contextRunner
			.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
					"spring.cloud.vault.session.lifecycle.refresh-before-expiry=11s",
					"spring.cloud.vault.session.lifecycle.expiry-threshold=12s")
			.withBean("vaultTokenSupplier", VaultTokenSupplier.class, () -> Mono::empty)
			.withBean("taskSchedulerWrapper", VaultAutoConfiguration.TaskSchedulerWrapper.class,
					() -> new VaultAutoConfiguration.TaskSchedulerWrapper(new ThreadPoolTaskScheduler()))
			.run(context -> {

				ReactiveSessionManager bean = context.getBean(ReactiveSessionManager.class);

				Object refreshTrigger = ReflectionTestUtils.getField(bean, "refreshTrigger");

				assertThat(refreshTrigger).hasFieldOrPropertyWithValue("duration", Duration.ofSeconds(11))
					.hasFieldOrPropertyWithValue("expiryThreshold", Duration.ofSeconds(12));
			});
	}

	@Test
	public void shouldConfigureEndpointProvider() {

		this.contextRunner
			.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
					"spring.cloud.vault.session.lifecycle.enabled=false")
			.withUserConfiguration(ReactiveEndpointProviderConfiguration.class)
			.withBean("vaultTokenSupplier", VaultTokenSupplier.class, () -> Mono::empty)
			.withBean("taskSchedulerWrapper", VaultAutoConfiguration.TaskSchedulerWrapper.class,
					() -> new VaultAutoConfiguration.TaskSchedulerWrapper(new ThreadPoolTaskScheduler()))
			.run(context -> {

				WebClientFactory factory = context.getBean(WebClientFactory.class);
				WebClient webClient = factory.create();

				webClient.get()
					.uri("foo")
					.retrieve()
					.bodyToMono(String.class)
					.as(StepVerifier::create)
					.verifyErrorMatches(throwable -> throwable.getMessage().contains("foobar-1"));

				webClient.get()
					.uri("foo")
					.retrieve()
					.bodyToMono(String.class)
					.as(StepVerifier::create)
					.verifyErrorMatches(throwable -> throwable.getMessage().contains("foobar-2"));
			});
	}

	@Test
	public void shouldConsiderCustomConnector() {

		this.contextRunner
			.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
					"spring.cloud.vault.session.lifecycle.enabled=false")
			.withUserConfiguration(CustomConnector.class)
			.withBean("vaultTokenSupplier", VaultTokenSupplier.class,
					() -> () -> Mono.just(VaultToken.of("foo".toCharArray())))
			.withBean("taskSchedulerWrapper", VaultAutoConfiguration.TaskSchedulerWrapper.class,
					() -> new VaultAutoConfiguration.TaskSchedulerWrapper(new ThreadPoolTaskScheduler()))
			.run(context -> {

				ReactiveVaultOperations operations = context.getBean(ReactiveVaultOperations.class);
				operations.delete("foo").as(StepVerifier::create).verifyError(WebClientRequestException.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class AuthenticationFactoryConfiguration {

		@Bean
		AuthenticationStepsFactory authenticationStepsFactory() {
			return () -> AuthenticationSteps.just(VaultToken.of("foo"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TokenSupplierConfiguration {

		@Bean
		VaultTokenSupplier vaultTokenSupplier() {
			AtomicLong counter = new AtomicLong();
			return () -> Mono.just(VaultToken.of("token-" + counter.incrementAndGet()));
		}

	}

	@Configuration
	static class CustomSessionManager {

		@Bean
		ReactiveSessionManager reactiveVaultSessionManager(VaultTokenSupplier tokenSupplier) {
			return tokenSupplier::getVaultToken;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveEndpointProviderConfiguration {

		@Bean
		ReactiveVaultEndpointProvider reactiveVaultEndpointProvider() {

			AtomicLong counter = new AtomicLong();

			return () -> Mono.fromSupplier(() -> {

				VaultEndpoint vaultEndpoint = new VaultEndpoint();
				vaultEndpoint.setHost("foobar-" + counter.incrementAndGet());

				return vaultEndpoint;
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConnector {

		@Bean
		VaultReactiveAutoConfiguration.ClientHttpConnectorWrapper myWrapper() {
			ClientHttpConnector mock = mock(ClientHttpConnector.class);
			when(mock.connect(any(), any(), any())).thenReturn(Mono.error(new UnsupportedOperationException()));

			return new VaultReactiveAutoConfiguration.ClientHttpConnectorWrapper(mock);
		}

	}

}
