/*
 * Copyright 2018-present the original author or authors.
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
import java.util.List;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationStepsOperator;
import org.springframework.vault.authentication.CachingVaultTokenSupplier;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ReactiveLifecycleAwareSessionManager;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.authentication.event.AuthenticationErrorEvent;
import org.springframework.vault.authentication.event.AuthenticationErrorListener;
import org.springframework.vault.authentication.event.AuthenticationEvent;
import org.springframework.vault.authentication.event.AuthenticationEventMulticaster;
import org.springframework.vault.authentication.event.AuthenticationListener;
import org.springframework.vault.client.ClientHttpConnectorFactory;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.client.WebClientFactory;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Support class for Vault configuration providing utility methods.
 *
 * @author Mark Paluch
 * @since 3.0
 */
final class VaultReactiveConfiguration {

	private final VaultProperties vaultProperties;

	VaultReactiveConfiguration(VaultProperties vaultProperties) {
		this.vaultProperties = vaultProperties;
	}

	ClientHttpConnector createClientHttpConnector() {

		ClientOptions clientOptions = new ClientOptions(Duration.ofMillis(this.vaultProperties.getConnectionTimeout()),
				Duration.ofMillis(this.vaultProperties.getReadTimeout()));

		SslConfiguration sslConfiguration = VaultConfiguration.createSslConfiguration(this.vaultProperties.getSsl());

		return ClientHttpConnectorFactory.create(clientOptions, sslConfiguration);
	}

	WebClientBuilder createWebClientBuilder(ClientHttpConnector connector,
			ReactiveVaultEndpointProvider endpointProvider, List<WebClientCustomizer> customizers) {

		WebClientBuilder builder = WebClientBuilder.builder()
			.httpConnector(connector)
			.endpointProvider(endpointProvider);

		return applyCustomizer(customizers, builder);
	}

	WebClientBuilder createWebClientBuilder(ClientHttpConnector connector, VaultEndpointProvider endpointProvider,
			List<WebClientCustomizer> customizers) {

		WebClientBuilder builder = WebClientBuilder.builder()
			.httpConnector(connector)
			.endpointProvider(endpointProvider);

		return applyCustomizer(customizers, builder);
	}

	private WebClientBuilder applyCustomizer(List<WebClientCustomizer> customizers, WebClientBuilder builder) {
		customizers.forEach(builder::customizers);

		if (StringUtils.hasText(this.vaultProperties.getNamespace())) {
			builder.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, this.vaultProperties.getNamespace());
		}

		return builder;
	}

	VaultTokenSupplier createVaultTokenSupplier(WebClientFactory webClientFactory,
			Supplier<AuthenticationStepsFactory> stepsFactorySupplier,
			Supplier<ClientAuthentication> clientAuthenticationSupplier) {

		AuthenticationStepsFactory authenticationStepsFactory = stepsFactorySupplier.get();
		if (authenticationStepsFactory != null) {
			return createAuthenticationStepsOperator(authenticationStepsFactory, webClientFactory);
		}

		ClientAuthentication clientAuthentication = clientAuthenticationSupplier.get();

		if (clientAuthentication != null) {

			if (clientAuthentication instanceof TokenAuthentication) {

				TokenAuthentication authentication = (TokenAuthentication) clientAuthentication;
				return () -> Mono.just(authentication.login());
			}

			if (clientAuthentication instanceof AuthenticationStepsFactory) {
				return createAuthenticationStepsOperator((AuthenticationStepsFactory) clientAuthentication,
						webClientFactory);
			}

			throw new IllegalStateException(String.format("Cannot construct VaultTokenSupplier from %s. "
					+ "ClientAuthentication must implement AuthenticationStepsFactory or be TokenAuthentication",
					clientAuthentication));
		}

		throw new IllegalStateException(
				"Cannot construct VaultTokenSupplier. Please configure VaultTokenSupplier bean named vaultTokenSupplier.");
	}

	private VaultTokenSupplier createAuthenticationStepsOperator(AuthenticationStepsFactory factory,
			WebClientFactory webClientFactory) {
		WebClient webClient = webClientFactory.create();
		return new AuthenticationStepsOperator(factory.getAuthenticationSteps(), webClient);
	}

	SessionManager createSessionManager(ReactiveSessionManager sessionManager) {
		return sessionManager instanceof AuthenticationEventMulticaster
				? new ReactiveMulticastingSessionManagerAdapter(sessionManager)
				: new ReactiveSessionManagerAdapter(sessionManager);
	}

	ReactiveSessionManager createReactiveSessionManager(VaultTokenSupplier vaultTokenSupplier,
			Supplier<TaskScheduler> taskScheduler, WebClientFactory webClientFactory) {

		VaultProperties.SessionLifecycle lifecycle = this.vaultProperties.getSession().getLifecycle();

		if (lifecycle.isEnabled()) {
			WebClient webClient = webClientFactory.create();
			ReactiveLifecycleAwareSessionManager.RefreshTrigger trigger = new ReactiveLifecycleAwareSessionManager.FixedTimeoutRefreshTrigger(
					lifecycle.getRefreshBeforeExpiry(), lifecycle.getExpiryThreshold());
			return new ReactiveLifecycleAwareSessionManager(vaultTokenSupplier, taskScheduler.get(), webClient,
					trigger);
		}

		return CachingVaultTokenSupplier.of(vaultTokenSupplier);
	}

	@SuppressWarnings("all")
	static class ReactiveSessionManagerAdapter implements SessionManager {

		private final ReactiveSessionManager sessionManager;

		ReactiveSessionManagerAdapter(ReactiveSessionManager sessionManager) {
			this.sessionManager = sessionManager;
		}

		@Override
		public VaultToken getSessionToken() {
			VaultToken token = this.sessionManager.getSessionToken().block();
			Assert.state(token != null, "ReactiveSessionManager returned a null VaultToken");
			return token;
		}

	}

	@SuppressWarnings("all")
	static class ReactiveMulticastingSessionManagerAdapter extends ReactiveSessionManagerAdapter
			implements AuthenticationEventMulticaster {

		private final AuthenticationEventMulticaster delegate;

		ReactiveMulticastingSessionManagerAdapter(ReactiveSessionManager sessionManager) {
			super(sessionManager);
			this.delegate = (AuthenticationEventMulticaster) sessionManager;
		}

		@Override
		public void addAuthenticationListener(AuthenticationListener listener) {
			this.delegate.addAuthenticationListener(listener);
		}

		@Override
		public void removeAuthenticationListener(AuthenticationListener listener) {
			this.delegate.removeAuthenticationListener(listener);
		}

		@Override
		public void addErrorListener(AuthenticationErrorListener listener) {
			this.delegate.addErrorListener(listener);
		}

		@Override
		public void removeErrorListener(AuthenticationErrorListener listener) {
			this.delegate.removeErrorListener(listener);
		}

		@Override
		public void multicastEvent(AuthenticationEvent event) {
			this.delegate.multicastEvent(event);
		}

		@Override
		public void multicastEvent(AuthenticationErrorEvent event) {
			this.delegate.multicastEvent(event);
		}

	}

}
