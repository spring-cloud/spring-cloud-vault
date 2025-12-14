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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ReactiveLifecycleAwareSessionManager;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.ReactiveVaultClient;
import org.springframework.vault.client.ReactiveVaultClientCustomizer;
import org.springframework.vault.client.ReactiveVaultClients;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.client.WebClientFactory;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.cloud.vault.config.VaultAutoConfiguration.TaskSchedulerWrapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive Spring Vault support.
 * <p>
 * This auto-configuration only supports static endpoints without
 * {@link VaultEndpointProvider} support as endpoint providers could be potentially
 * blocking implementations.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnExpression("${spring.cloud.vault.reactive.enabled:true}")
@ConditionalOnClass({ Flux.class, WebClient.class, ReactiveVaultOperations.class, HttpClient.class })
@EnableConfigurationProperties({ VaultProperties.class })
@AutoConfigureBefore(VaultAutoConfiguration.class)
public class VaultReactiveAutoConfiguration implements InitializingBean {

	@Nullable
	private final ConfigurableApplicationContext applicationContext;

	private final VaultProperties vaultProperties;

	private final VaultReactiveConfiguration configuration;

	@Nullable
	private final ReactiveVaultEndpointProvider reactiveEndpointProvider;

	@Nullable
	private final VaultEndpointProvider endpointProvider;

	private final List<ReactiveVaultClientCustomizer> vaultClientCustomizers;

	private final List<WebClientCustomizer> customizers;

	/**
	 * @deprecated since 3.0.2, use
	 * {@link #VaultReactiveAutoConfiguration(ConfigurableApplicationContext, VaultProperties, ObjectProvider, ObjectProvider, ObjectProvider, ObjectProvider)}
	 * instead.
	 */
	@Deprecated
	public VaultReactiveAutoConfiguration(VaultProperties vaultProperties,
			ObjectProvider<ReactiveVaultEndpointProvider> reactiveEndpointProvider,
			ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<ReactiveVaultClientCustomizer>> vaultClientCustomizers,
			ObjectProvider<List<WebClientCustomizer>> webClientCustomizers) {
		this(null, vaultProperties, reactiveEndpointProvider, endpointProvider, vaultClientCustomizers,
				webClientCustomizers);
	}

	@Autowired
	public VaultReactiveAutoConfiguration(@Nullable ConfigurableApplicationContext applicationContext,
			VaultProperties vaultProperties, ObjectProvider<ReactiveVaultEndpointProvider> reactiveEndpointProvider,
			ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<ReactiveVaultClientCustomizer>> vaultClientCustomizers,
			ObjectProvider<List<WebClientCustomizer>> webClientCustomizers) {

		this.applicationContext = applicationContext;
		this.vaultProperties = vaultProperties;
		this.configuration = new VaultReactiveConfiguration(vaultProperties);

		this.reactiveEndpointProvider = reactiveEndpointProvider.getIfAvailable();

		if (this.reactiveEndpointProvider == null) {
			this.endpointProvider = endpointProvider.getIfAvailable(() -> SimpleVaultEndpointProvider
				.of(new VaultConfiguration(vaultProperties).createVaultEndpoint()));
		}
		else {
			this.endpointProvider = null;
		}

		this.vaultClientCustomizers = new ArrayList<>(vaultClientCustomizers.getIfAvailable(Collections::emptyList));
		AnnotationAwareOrderComparator.sort(this.vaultClientCustomizers);
		this.customizers = new ArrayList<>(webClientCustomizers.getIfAvailable(Collections::emptyList));
		AnnotationAwareOrderComparator.sort(this.customizers);
	}

	protected WebClientBuilder webClientBuilder(ClientHttpConnector connector) {

		if (this.reactiveEndpointProvider != null) {
			return this.configuration.createWebClientBuilder(connector, this.reactiveEndpointProvider,
					this.customizers);
		}

		if (this.endpointProvider != null) {
			return this.configuration.createWebClientBuilder(connector, this.endpointProvider, this.customizers);
		}

		throw new IllegalStateException(
				"Cannot create WebClientBuilder as neither ReactiveEndpointProvider nor EndpointProvider configured");
	}

	/**
	 * Creates a {@link ClientHttpConnectorWrapper} containing a
	 * {@link ClientHttpConnector}. {@link ClientHttpConnector} is not exposed as root
	 * bean because {@link ClientHttpConnector} is configured with {@link ClientOptions}
	 * and {@link SslConfiguration} which are not necessarily applicable for the whole
	 * application.
	 * @return the {@link ClientHttpConnectorWrapper} to wrap a
	 * {@link ClientHttpConnector} instance.
	 * @since 3.0.2
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClientHttpConnectorWrapper clientHttpConnectorWrapper() {
		return new ClientHttpConnectorWrapper(createConnector(this.vaultProperties));
	}

	@Override
	public void afterPropertiesSet() {
	}

	/**
	 * Creates a {@link ClientHttpConnector} configured with {@link ClientOptions} and
	 * {@link SslConfiguration} which are not necessarily applicable for the whole
	 * application.
	 * @param vaultProperties the Vault properties.
	 * @return the {@link ClientHttpConnector}.
	 */
	protected ClientHttpConnector createConnector(VaultProperties vaultProperties) {
		return new VaultReactiveConfiguration(vaultProperties).createClientHttpConnector();
	}

	/**
	 * Create a {@link WebClientFactory} bean that is used to produce {@link WebClient}.
	 * @return the {@link WebClientFactory}.
	 * @since 3.0
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebClientFactory vaultWebClientFactory() {

		ClientHttpConnector connector = getClientHttpConnector();

		return new DefaultWebClientFactory(connector, this::webClientBuilder);
	}

	protected ClientHttpConnector getClientHttpConnector() {

		if (this.applicationContext != null) {
			return this.applicationContext.getBean(ClientHttpConnectorWrapper.class).getConnector();
		}

		return createConnector(this.vaultProperties);
	}

	/**
	 * Creates a {@link VaultTemplate} using a {@link VaultClient}.
	 * @return the {@link VaultClient} bean.
	 * @since 5.1
	 * @see VaultClient
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveVaultClient.class)
	public ReactiveVaultClient reactiveVaultClient() {
		WebClient webClient = getWebClient();
		ReactiveVaultEndpointProvider endpointProvider;
		if (this.reactiveEndpointProvider != null) {
			endpointProvider = this.reactiveEndpointProvider;
		}
		else if (this.endpointProvider != null) {
			endpointProvider = ReactiveVaultClients.wrap(this.endpointProvider);
		}
		else {
			throw new IllegalStateException("No ReactiveVaultEndpointProvider or VaultEndpointProvider configured");
		}
		return this.configuration.createVaultClient(this.vaultClientCustomizers, webClient, endpointProvider);
	}

	private WebClient getWebClient() {
		return webClientBuilder(getClientHttpConnector()).build();
	}

	/**
	 * Creates a {@link ReactiveVaultTemplate}.
	 * @param sessionManager object provider for {@link ReactiveSessionManager}.
	 * @return the {@link ReactiveVaultTemplate} bean.
	 * @since 5.1
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveVaultOperations.class)
	public ReactiveVaultTemplate reactiveVaultTemplate(ReactiveVaultClient reactiveVaultClient,
			ObjectProvider<ReactiveSessionManager> sessionManager) {

		if (this.vaultProperties.getAuthentication() == VaultProperties.AuthenticationMethod.NONE) {
			return new ReactiveVaultTemplate(reactiveVaultClient);
		}

		return new ReactiveVaultTemplate(reactiveVaultClient, sessionManager.getObject());
	}

	/**
	 * @param beanFactory the {@link BeanFactory}.
	 * @param asyncTaskExecutorFactory the {@link ObjectFactory} for
	 * {@link TaskSchedulerWrapper}.
	 * @param reactiveVaultClient the {@link ReactiveVaultClient}.
	 * @return {@link ReactiveSessionManager} for reactive session use.
	 * @since 5.1
	 * @see ReactiveSessionManager
	 * @see ReactiveLifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAuthentication
	public ReactiveSessionManager reactiveVaultSessionManager(BeanFactory beanFactory,
			ObjectFactory<TaskSchedulerWrapper> asyncTaskExecutorFactory, ReactiveVaultClient reactiveVaultClient) {

		VaultTokenSupplier vaultTokenSupplier = beanFactory.getBean("vaultTokenSupplier", VaultTokenSupplier.class);

		return this.configuration.createReactiveSessionManager(vaultTokenSupplier,
				() -> asyncTaskExecutorFactory.getObject().getTaskScheduler(), reactiveVaultClient);
	}

	/**
	 * @param sessionManager the {@link ReactiveSessionManager}.
	 * @return {@link SessionManager} adapter wrapping {@link ReactiveSessionManager}.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAuthentication
	public SessionManager vaultSessionManager(ReactiveSessionManager sessionManager) {
		return this.configuration.createSessionManager(sessionManager);
	}

	/**
	 * @param beanFactory the {@link BeanFactory}.
	 * @param reactiveVaultClient the {@link ReactiveVaultClient}.
	 * @return the {@link VaultTokenSupplier} for reactive Vault session management
	 * adapting {@link ClientAuthentication} that also implement
	 * {@link AuthenticationStepsFactory}.
	 * @since 5.1
	 * @see AuthenticationStepsFactory
	 */
	@Bean
	@ConditionalOnMissingBean(name = "vaultTokenSupplier")
	@ConditionalOnAuthentication
	public VaultTokenSupplier vaultTokenSupplier(ListableBeanFactory beanFactory,
			ReactiveVaultClient reactiveVaultClient) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		WebClient webClient = getWebClient();
		return this.configuration.createVaultTokenSupplier(reactiveVaultClient, webClient,
				() -> beanFactory.getBeanProvider(AuthenticationStepsFactory.class, false).getIfAvailable(),
				() -> beanFactory.getBeanProvider(ClientAuthentication.class, false).getIfAvailable());
	}

	/**
	 * Wrapper for {@link ClientHttpConnector} to not expose the bean globally.
	 *
	 * @since 3.0.2
	 */
	public static class ClientHttpConnectorWrapper implements InitializingBean, DisposableBean {

		private final ClientHttpConnector connector;

		public ClientHttpConnectorWrapper(ClientHttpConnector connector) {
			this.connector = connector;
		}

		@Override
		public void destroy() throws Exception {
			if (this.connector instanceof DisposableBean) {
				((DisposableBean) this.connector).destroy();
			}
		}

		@Override
		public void afterPropertiesSet() throws Exception {

			if (this.connector instanceof InitializingBean) {
				((InitializingBean) this.connector).afterPropertiesSet();
			}
		}

		public ClientHttpConnector getConnector() {
			return this.connector;
		}

	}

}
