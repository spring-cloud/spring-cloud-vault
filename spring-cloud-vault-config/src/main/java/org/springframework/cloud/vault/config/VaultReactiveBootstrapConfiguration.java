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

import java.time.Duration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.config.VaultBootstrapConfiguration.TaskSchedulerWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationStepsOperator;
import org.springframework.vault.authentication.CachingVaultTokenSupplier;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ReactiveLifecycleAwareSessionManager;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.ReactiveVaultClients;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.ClientHttpConnectorFactory;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive Spring Vault support.
 * <p>
 * This auto-configuration only supports static endpoints without
 * {@link org.springframework.vault.client.VaultEndpointProvider} support as endpoint
 * providers could be potentially blocking implementations.
 *
 * @author Mark Paluch
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnExpression("${spring.cloud.vault.reactive.enabled:true}")
@ConditionalOnClass({ Flux.class, WebClient.class, ReactiveVaultOperations.class })
@EnableConfigurationProperties({ VaultProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class VaultReactiveBootstrapConfiguration {

	private final VaultProperties vaultProperties;

	private final VaultEndpoint vaultEndpoint;

	private final ClientHttpConnector clientHttpConnector;

	public VaultReactiveBootstrapConfiguration(VaultProperties vaultProperties) {

		this.vaultProperties = vaultProperties;
		this.vaultEndpoint = VaultConfigurationUtil.createVaultEndpoint(vaultProperties);
		this.clientHttpConnector = createConnector(this.vaultProperties);
	}

	/**
	 * Creates a {@link ClientHttpConnector} configured with {@link ClientOptions} and
	 * {@link SslConfiguration} which are not necessarily applicable for the whole
	 * application.
	 *
	 * @return the {@link ClientHttpConnector}.
	 */
	private static ClientHttpConnector createConnector(VaultProperties vaultProperties) {

		ClientOptions clientOptions = new ClientOptions(Duration.ofMillis(vaultProperties
				.getConnectionTimeout()), Duration.ofMillis(vaultProperties
				.getReadTimeout()));

		SslConfiguration sslConfiguration = VaultConfigurationUtil
				.createSslConfiguration(vaultProperties.getSsl());

		return ClientHttpConnectorFactory.create(clientOptions, sslConfiguration);
	}

	/**
	 * Creates a {@link ReactiveVaultTemplate}.
	 *
	 * @return
	 * @see #reactiveVaultSessionManager(BeanFactory, ObjectFactory)
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveVaultOperations.class)
	public ReactiveVaultTemplate reactiveVaultTemplate(
			ReactiveSessionManager tokenSupplier) {
		return new ReactiveVaultTemplate(vaultEndpoint, clientHttpConnector,
				tokenSupplier);
	}

	/**
	 * @return {@link ReactiveSessionManager} for reactive session use.
	 * @see ReactiveSessionManager
	 * @see ReactiveLifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	public ReactiveSessionManager reactiveVaultSessionManager(BeanFactory beanFactory,
			ObjectFactory<TaskSchedulerWrapper> asyncTaskExecutorFactory) {

		VaultTokenSupplier vaultTokenSupplier = beanFactory.getBean("vaultTokenSupplier",
				VaultTokenSupplier.class);

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {

			WebClient webClient = ReactiveVaultClients.createWebClient(vaultEndpoint,
					clientHttpConnector);
			return new ReactiveLifecycleAwareSessionManager(vaultTokenSupplier,
					asyncTaskExecutorFactory.getObject().getTaskScheduler(), webClient);
		}

		return CachingVaultTokenSupplier.of(vaultTokenSupplier);
	}

	/**
	 * @return {@link SessionManager} adapter wrapping {@link ReactiveSessionManager}.
	 */
	@Bean
	@ConditionalOnMissingBean
	public SessionManager vaultSessionManager(ReactiveSessionManager sessionManager) {
		return sessionManager.getSessionToken()::block;
	}

	/**
	 * @return the {@link VaultTokenSupplier} for reactive Vault session management
	 * adapting {@link ClientAuthentication} that also implement
	 * {@link AuthenticationStepsFactory}.
	 * @see AuthenticationStepsFactory
	 */
	@Bean
	@ConditionalOnMissingBean(name = "vaultTokenSupplier")
	public VaultTokenSupplier vaultTokenSupplier(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		String[] authStepsFactories = beanFactory
				.getBeanNamesForType(AuthenticationStepsFactory.class);

		if (!ObjectUtils.isEmpty(authStepsFactories)) {

			AuthenticationStepsFactory factory = beanFactory
					.getBean(AuthenticationStepsFactory.class);
			return createAuthenticationStepsOperator(factory);
		}

		String[] clientAuthentications = beanFactory
				.getBeanNamesForType(ClientAuthentication.class);

		if (!ObjectUtils.isEmpty(clientAuthentications)) {

			ClientAuthentication clientAuthentication = beanFactory
					.getBean(ClientAuthentication.class);

			if (clientAuthentication instanceof TokenAuthentication) {

				TokenAuthentication authentication = (TokenAuthentication) clientAuthentication;
				return () -> Mono.just(authentication.login());
			}

			if (clientAuthentication instanceof AuthenticationStepsFactory) {
				return createAuthenticationStepsOperator((AuthenticationStepsFactory) clientAuthentication);
			}

			throw new IllegalStateException(
					String.format(
							"Cannot construct VaultTokenSupplier from %s. "
									+ "ClientAuthentication must implement AuthenticationStepsFactory or be TokenAuthentication",
							clientAuthentication));
		}

		throw new IllegalStateException(
				"Cannot construct VaultTokenSupplier. Please configure VaultTokenSupplier bean named vaultTokenSupplier.");
	}

	private VaultTokenSupplier createAuthenticationStepsOperator(
			AuthenticationStepsFactory factory) {
		WebClient webClient = ReactiveVaultClients.createWebClient(this.vaultEndpoint,
				this.clientHttpConnector);
		return new AuthenticationStepsOperator(factory.getAuthenticationSteps(),
				webClient);
	}
}
