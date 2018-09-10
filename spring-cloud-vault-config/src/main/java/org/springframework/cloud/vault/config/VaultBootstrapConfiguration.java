/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultClients;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.config.ClientHttpRequestFactoryFactory;
import org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.client.RestOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VaultProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class VaultBootstrapConfiguration implements InitializingBean {

	private final ConfigurableApplicationContext applicationContext;

	private final VaultProperties vaultProperties;

	private final VaultEndpointProvider endpointProvider;

	private RestOperations restOperations;

	public VaultBootstrapConfiguration(ConfigurableApplicationContext applicationContext,
			VaultProperties vaultProperties,
			ObjectProvider<VaultEndpointProvider> endpointProvider) {

		this.applicationContext = applicationContext;
		this.vaultProperties = vaultProperties;

		VaultEndpointProvider provider = endpointProvider.getIfAvailable();

		if (provider == null) {
			provider = SimpleVaultEndpointProvider.of(VaultConfigurationUtil
					.createVaultEndpoint(vaultProperties));
		}

		this.endpointProvider = provider;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {

		ClientHttpRequestFactory clientHttpRequestFactory = clientHttpRequestFactoryWrapper()
				.getClientHttpRequestFactory();

		this.restOperations = VaultClients.createRestTemplate(endpointProvider,
				clientHttpRequestFactory);
	}

	/**
	 * Creates a {@link ClientFactoryWrapper} containing a
	 * {@link ClientHttpRequestFactory}. {@link ClientHttpRequestFactory} is not exposed
	 * as root bean because {@link ClientHttpRequestFactory} is configured with
	 * {@link ClientOptions} and {@link SslConfiguration} which are not necessarily
	 * applicable for the whole application.
	 *
	 * @return the {@link ClientFactoryWrapper} to wrap a {@link ClientHttpRequestFactory}
	 * instance.
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClientFactoryWrapper clientHttpRequestFactoryWrapper() {

		ClientOptions clientOptions = new ClientOptions(Duration.ofMillis(vaultProperties
				.getConnectionTimeout()), Duration.ofMillis(vaultProperties
				.getReadTimeout()));

		SslConfiguration sslConfiguration = VaultConfigurationUtil
				.createSslConfiguration(vaultProperties.getSsl());

		return new ClientFactoryWrapper(ClientHttpRequestFactoryFactory.create(
				clientOptions, sslConfiguration));
	}

	/**
	 * Creates a {@link VaultTemplate}.
	 *
	 * @return
	 * @see #clientHttpRequestFactoryWrapper()
	 */
	@Bean
	@ConditionalOnMissingBean(VaultOperations.class)
	public VaultTemplate vaultTemplate(SessionManager sessionManager) {
		return new VaultTemplate(endpointProvider, clientHttpRequestFactoryWrapper()
				.getClientHttpRequestFactory(), sessionManager);
	}

	/**
	 * Creates a new {@link TaskSchedulerWrapper} that encapsulates a bean implementing
	 * {@link TaskScheduler} and {@link AsyncTaskExecutor}.
	 *
	 * @return
	 * @see ThreadPoolTaskScheduler
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean(TaskSchedulerWrapper.class)
	public TaskSchedulerWrapper vaultTaskScheduler() {

		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setDaemon(true);
		threadPoolTaskScheduler.setThreadNamePrefix("Spring-Cloud-Vault-");

		// This is to destroy bootstrap resources
		// otherwise, the bootstrap context is not shut down cleanly
		applicationContext.registerShutdownHook();

		return new TaskSchedulerWrapper(threadPoolTaskScheduler);
	}

	/**
	 * @return the {@link SessionManager} for Vault session management.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	public SessionManager vaultSessionManager(ClientAuthentication clientAuthentication,
			ObjectFactory<TaskSchedulerWrapper> asyncTaskExecutorFactory) {

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {
			return new LifecycleAwareSessionManager(clientAuthentication,
					asyncTaskExecutorFactory.getObject().getTaskScheduler(),
					restOperations);
		}

		return new SimpleSessionManager(clientAuthentication);
	}

	/**
	 * @return the {@link ClientAuthentication} to obtain a
	 * {@link org.springframework.vault.support.VaultToken}.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClientAuthentication clientAuthentication() {

		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(
				vaultProperties, restOperations);

		return factory.createClientAuthentication();
	}

	/**
	 * Wrapper to keep {@link TaskScheduler} local to Spring Cloud Vault.
	 */
	public static class TaskSchedulerWrapper implements InitializingBean, DisposableBean {

		private final ThreadPoolTaskScheduler taskScheduler;

		public TaskSchedulerWrapper(ThreadPoolTaskScheduler taskScheduler) {
			this.taskScheduler = taskScheduler;
		}

		ThreadPoolTaskScheduler getTaskScheduler() {
			return taskScheduler;
		}

		@Override
		public void destroy() throws Exception {
			taskScheduler.destroy();
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			taskScheduler.afterPropertiesSet();
		}
	}
}
