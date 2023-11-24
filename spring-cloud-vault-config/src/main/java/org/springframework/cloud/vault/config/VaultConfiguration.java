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

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.vault.config.VaultProperties.Ssl;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.LifecycleAwareSessionManagerSupport;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.authentication.event.AuthenticationEventMulticaster;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.RestTemplateCustomizer;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.client.RestTemplateRequestCustomizer;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.LeaseStrategy;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;
import org.springframework.web.client.RestTemplate;

/**
 * Support class for Vault configuration providing utility methods.
 *
 * @author Mark Paluch
 * @since 3.0
 */
final class VaultConfiguration {

	private final VaultProperties vaultProperties;

	VaultConfiguration(VaultProperties vaultProperties) {
		this.vaultProperties = vaultProperties;
	}

	/**
	 * Create a {@link SslConfiguration} given {@link Ssl SSL properties}.
	 * @param ssl the SSL properties.
	 * @return the SSL configuration.
	 */
	static SslConfiguration createSslConfiguration(Ssl ssl) {

		KeyStoreConfiguration keyStore = KeyStoreConfiguration.unconfigured();
		KeyStoreConfiguration trustStore = KeyStoreConfiguration.unconfigured();

		if (ssl.getKeyStore() != null) {
			if (StringUtils.hasText(ssl.getKeyStorePassword())) {
				keyStore = KeyStoreConfiguration.of(ssl.getKeyStore(), ssl.getKeyStorePassword().toCharArray());
			}
			else {
				keyStore = KeyStoreConfiguration.of(ssl.getKeyStore());
			}

			if (StringUtils.hasText(ssl.getKeyStoreType())) {
				keyStore = keyStore.withStoreType(ssl.getKeyStoreType());
			}
		}

		if (ssl.getTrustStore() != null) {

			if (StringUtils.hasText(ssl.getTrustStorePassword())) {
				trustStore = KeyStoreConfiguration.of(ssl.getTrustStore(), ssl.getTrustStorePassword().toCharArray());
			}
			else {
				trustStore = KeyStoreConfiguration.of(ssl.getTrustStore());
			}

			if (StringUtils.hasText(ssl.getTrustStoreType())) {
				trustStore = trustStore.withStoreType(ssl.getTrustStoreType());
			}
		}

		return new SslConfiguration(keyStore, trustStore, ssl.getEnabledProtocols(), ssl.getEnabledCipherSuites());
	}

	ClientHttpRequestFactory createClientHttpRequestFactory() {

		ClientOptions clientOptions = new ClientOptions(Duration.ofMillis(this.vaultProperties.getConnectionTimeout()),
				Duration.ofMillis(this.vaultProperties.getReadTimeout()));

		SslConfiguration sslConfiguration = VaultConfiguration.createSslConfiguration(this.vaultProperties.getSsl());

		return ClientHttpRequestFactoryFactory.create(clientOptions, sslConfiguration);
	}

	/**
	 * Create a {@link VaultEndpoint} from {@link VaultProperties}.
	 * @return the endpoint.
	 */
	VaultEndpoint createVaultEndpoint() {

		if (StringUtils.hasText(this.vaultProperties.getUri())) {
			return VaultEndpoint.from(URI.create(this.vaultProperties.getUri()));
		}

		VaultEndpoint vaultEndpoint = new VaultEndpoint();
		vaultEndpoint.setHost(this.vaultProperties.getHost());
		vaultEndpoint.setPort(this.vaultProperties.getPort());
		vaultEndpoint.setScheme(this.vaultProperties.getScheme());

		return vaultEndpoint;
	}

	VaultEndpoint createVaultEndpoint(ServiceInstance server) {
		String fallbackScheme;

		if (StringUtils.hasText(this.vaultProperties.getUri())) {
			fallbackScheme = URI.create(this.vaultProperties.getUri()).getScheme();
		}
		else {
			fallbackScheme = this.vaultProperties.getScheme();
		}

		VaultEndpoint vaultEndpoint = VaultEndpoint.create(server.getHost(), server.getPort());

		if (server.getMetadata().containsKey("scheme")) {
			vaultEndpoint.setScheme(server.getMetadata().get("scheme"));
		}
		else {
			vaultEndpoint.setScheme(server.isSecure() ? "https" : fallbackScheme);
		}
		return vaultEndpoint;
	}

	RestTemplateBuilder createRestTemplateBuilder(ClientHttpRequestFactory requestFactory,
			VaultEndpointProvider endpointProvider, List<RestTemplateCustomizer> customizers,
			List<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		RestTemplateBuilder builder = RestTemplateBuilder.builder().requestFactory(requestFactory)
				.endpointProvider(endpointProvider);

		customizers.forEach(builder::customizers);
		requestCustomizers.forEach(builder::requestCustomizers);

		if (StringUtils.hasText(this.vaultProperties.getNamespace())) {
			builder.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, this.vaultProperties.getNamespace());
		}
		return builder;
	}

	SessionManager createSessionManager(ClientAuthentication clientAuthentication,
			Supplier<TaskScheduler> taskSchedulerSupplier, RestTemplateFactory restTemplateFactory) {
		VaultProperties.SessionLifecycle lifecycle = this.vaultProperties.getSession().getLifecycle();

		if (lifecycle.isEnabled()) {
			RestTemplate restTemplate = restTemplateFactory.create();
			LifecycleAwareSessionManagerSupport.RefreshTrigger trigger = new LifecycleAwareSessionManagerSupport.FixedTimeoutRefreshTrigger(
					lifecycle.getRefreshBeforeExpiry(), lifecycle.getExpiryThreshold());
			return new LifecycleAwareSessionManager(clientAuthentication, taskSchedulerSupplier.get(), restTemplate,
					trigger);
		}

		return new SimpleSessionManager(clientAuthentication);
	}

	SecretLeaseContainer createSecretLeaseContainer(VaultOperations vaultOperations,
			Supplier<TaskScheduler> taskSchedulerSupplier, @Nullable SessionManager sessionManager) {

		VaultProperties.ConfigLifecycle lifecycle = this.vaultProperties.getConfig().getLifecycle();

		SecretLeaseContainer container = new SecretLeaseContainer(vaultOperations, taskSchedulerSupplier.get());

		if (sessionManager instanceof AuthenticationEventMulticaster am) {
			am.addAuthenticationListener(container.getAuthenticationListener());
			am.addErrorListener(container.getAuthenticationErrorListener());
		}

		customizeContainer(lifecycle, container);

		return container;
	}

	static ThreadPoolTaskScheduler createScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setDaemon(true);
		threadPoolTaskScheduler.setThreadNamePrefix("Spring-Cloud-Vault-");
		return threadPoolTaskScheduler;
	}

	static void customizeContainer(VaultProperties.ConfigLifecycle lifecycle, SecretLeaseContainer container) {

		if (lifecycle.isEnabled()) {

			if (lifecycle.getMinRenewal() != null) {
				container.setMinRenewal(lifecycle.getMinRenewal());
			}

			if (lifecycle.getExpiryThreshold() != null) {
				container.setExpiryThreshold(lifecycle.getExpiryThreshold());
			}

			if (lifecycle.getLeaseEndpoints() != null) {
				container.setLeaseEndpoints(lifecycle.getLeaseEndpoints());
			}

			if (lifecycle.getLeaseStrategy() != null) {

				switch (lifecycle.getLeaseStrategy()) {
					case DropOnError -> container.setLeaseStrategy(LeaseStrategy.dropOnError());
					case RetainOnError -> container.setLeaseStrategy(LeaseStrategy.retainOnError());
					case RetainOnIoError -> container.setLeaseStrategy(LeaseStrategy.retainOnIoError());
				}
			}
		}
	}

}
