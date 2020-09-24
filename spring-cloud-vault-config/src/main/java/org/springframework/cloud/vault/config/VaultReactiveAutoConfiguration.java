/*
 * Copyright 2017-2020 the original author or authors.
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.client.WebClientFactory;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.ReactiveVaultTemplate;
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

	private final VaultProperties vaultProperties;

	private final VaultReactiveConfiguration configuration;

	private final VaultEndpointProvider endpointProvider;

	private final List<WebClientCustomizer> customizers;

	@Nullable
	private ClientHttpConnector clientHttpConnector;

	/**
	 * Used for Vault communication.
	 */
	@Nullable
	private WebClientBuilder webClientBuilder;

	public VaultReactiveAutoConfiguration(VaultProperties vaultProperties,
			ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<WebClientCustomizer>> webClientCustomizers) {

		this.vaultProperties = vaultProperties;
		this.configuration = new VaultReactiveConfiguration(vaultProperties);

		this.endpointProvider = endpointProvider.getIfAvailable(
				() -> SimpleVaultEndpointProvider.of(new VaultConfiguration(vaultProperties).createVaultEndpoint()));
		this.customizers = new ArrayList<>(webClientCustomizers.getIfAvailable(Collections::emptyList));
		AnnotationAwareOrderComparator.sort(this.customizers);
	}

	@Override
	public void afterPropertiesSet() {

		this.clientHttpConnector = createConnector(this.vaultProperties);

		this.webClientBuilder = webClientBuilder(this.clientHttpConnector);
	}

	protected WebClientBuilder webClientBuilder(ClientHttpConnector connector) {
		return this.configuration.createWebClientBuilder(connector, this.endpointProvider, this.customizers);
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

		Assert.state(this.clientHttpConnector != null, "ClientHttpConnector must not be null");

		return new DefaultWebClientFactory(this.clientHttpConnector, this::webClientBuilder);
	}

	/**
	 * Creates a {@link ReactiveVaultTemplate}.
	 * @param sessionManager object provider for {@link ReactiveSessionManager}.
	 * @return the {@link ReactiveVaultTemplate} bean.
	 * @see #reactiveVaultSessionManager(BeanFactory, ObjectFactory, WebClientFactory)
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveVaultOperations.class)
	public ReactiveVaultTemplate reactiveVaultTemplate(ObjectProvider<ReactiveSessionManager> sessionManager) {

		Assert.state(this.webClientBuilder != null, "WebClientBuilder must not be null");

		if (this.vaultProperties.getAuthentication() == VaultProperties.AuthenticationMethod.NONE) {
			return new ReactiveVaultTemplate(this.webClientBuilder);
		}

		return new ReactiveVaultTemplate(this.webClientBuilder, sessionManager.getObject());
	}

	/**
	 * @param beanFactory the {@link BeanFactory}.
	 * @param asyncTaskExecutorFactory the {@link ObjectFactory} for
	 * {@link TaskSchedulerWrapper}.
	 * @param webClientFactory the web client factory
	 * @return {@link ReactiveSessionManager} for reactive session use.
	 * @see ReactiveSessionManager
	 * @see ReactiveLifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAuthentication
	public ReactiveSessionManager reactiveVaultSessionManager(BeanFactory beanFactory,
			ObjectFactory<TaskSchedulerWrapper> asyncTaskExecutorFactory, WebClientFactory webClientFactory) {

		VaultTokenSupplier vaultTokenSupplier = beanFactory.getBean("vaultTokenSupplier", VaultTokenSupplier.class);

		return this.configuration.createReactiveSessionManager(vaultTokenSupplier,
				() -> asyncTaskExecutorFactory.getObject().getTaskScheduler(), webClientFactory);
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
	 * @param webClientFactory the {@link WebClientFactory}.
	 * @return the {@link VaultTokenSupplier} for reactive Vault session management
	 * adapting {@link ClientAuthentication} that also implement
	 * {@link AuthenticationStepsFactory}.
	 * @see AuthenticationStepsFactory
	 */
	@Bean
	@ConditionalOnMissingBean(name = "vaultTokenSupplier")
	@ConditionalOnAuthentication
	public VaultTokenSupplier vaultTokenSupplier(ListableBeanFactory beanFactory, WebClientFactory webClientFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		return this.configuration.createVaultTokenSupplier(webClientFactory,
				() -> beanFactory.getBeanProvider(AuthenticationStepsFactory.class, false).getIfAvailable(),
				() -> beanFactory.getBeanProvider(ClientAuthentication.class, false).getIfAvailable());
	}

}
