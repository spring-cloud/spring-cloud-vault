/*
 * Copyright 2016 the original author or authors.
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

import java.net.URI;
import java.util.Collection;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.*;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.ClientHttpRequestFactoryFactory;
import org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;
import org.springframework.vault.core.DefaultVaultClientFactory;
import org.springframework.vault.core.VaultClientFactory;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.VaultToken;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ VaultProperties.class,
		VaultGenericBackendProperties.class })
public class VaultBootstrapConfiguration implements InitializingBean {

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Autowired
	private VaultProperties vaultProperties;

	private Collection<VaultSecretBackendDescriptor> vaultSecretBackendDescriptors;

	private Collection<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories;

	@Override
	public void afterPropertiesSet() throws Exception {

		this.vaultSecretBackendDescriptors = applicationContext.getBeansOfType(
				VaultSecretBackendDescriptor.class).values();

		this.factories = (Collection) applicationContext.getBeansOfType(
				SecretBackendMetadataFactory.class).values();
	}

	@Bean
	public VaultPropertySourceLocator vaultPropertySourceLocator(
			VaultOperations operations,
			VaultProperties vaultProperties,
			VaultGenericBackendProperties vaultGenericBackendProperties,
			ObjectFactory<TaskSchedulerWrapper<? extends TaskScheduler>> taskSchedulerFactory) {

		Collection<SecretBackendMetadata> backendAccessors = SecretBackendFactories
				.createSecretBackendMetadata(vaultSecretBackendDescriptors, factories);
		VaultConfigTemplate vaultConfigTemplate = new VaultConfigTemplate(operations,
				vaultProperties);

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {

			// This is to destroy bootstrap resources
			// otherwise, the bootstrap context is not shut down cleanly
			applicationContext.registerShutdownHook();

			return new LeasingVaultPropertySourceLocator(vaultConfigTemplate,
					vaultProperties, vaultGenericBackendProperties, backendAccessors,
					taskSchedulerFactory.getObject().getTaskScheduler());
		}

		return new VaultPropertySourceLocator(vaultConfigTemplate, vaultProperties,
				vaultGenericBackendProperties, backendAccessors);
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

		ClientOptions clientOptions = new ClientOptions(
				vaultProperties.getConnectionTimeout(), vaultProperties.getReadTimeout());

		VaultProperties.Ssl ssl = vaultProperties.getSsl();
		SslConfiguration sslConfiguration;
		if (ssl != null) {
			sslConfiguration = new SslConfiguration(ssl.getKeyStore(),
					ssl.getKeyStorePassword(), ssl.getTrustStore(),
					ssl.getTrustStorePassword());
		}
		else {
			sslConfiguration = SslConfiguration.NONE;
		}

		return new ClientFactoryWrapper(ClientHttpRequestFactoryFactory.create(
				clientOptions, sslConfiguration));
	}

	/**
	 * @return the {@link VaultClient}
	 * @see #clientHttpRequestFactoryWrapper()
	 */
	@Bean
	@ConditionalOnMissingBean
	public VaultClient vaultClient() {

		VaultEndpoint vaultEndpoint = new VaultEndpoint();
		vaultEndpoint.setHost(vaultProperties.getHost());
		vaultEndpoint.setPort(vaultProperties.getPort());
		vaultEndpoint.setScheme(vaultProperties.getScheme());

		return new VaultClient(clientHttpRequestFactoryWrapper()
				.getClientHttpRequestFactory(), vaultEndpoint);
	}

	/**
	 * Creates the {@link VaultClientFactory} to be used with {@link VaultTemplate}. Uses
	 * by default {@link DefaultVaultClientFactory} with the configured
	 * {@link #vaultClient()} instance.
	 *
	 * @return
	 */
	@Bean
	@ConditionalOnMissingBean
	public VaultClientFactory vaultClientFactory() {
		return new DefaultVaultClientFactory(vaultClient());
	}

	/**
	 * Creates a {@link VaultTemplate}.
	 *
	 * @return
	 * @see #vaultClientFactory()
	 */
	@Bean
	@ConditionalOnMissingBean
	public VaultTemplate vaultTemplate(ClientAuthentication clientAuthentication,
			SessionManager sessionManager) {
		return new VaultTemplate(vaultClientFactory(), sessionManager);
	}

	/**
	 * Creates a new {@link TaskSchedulerWrapper} that encapsulates a bean implementing
	 * {@link TaskScheduler} and {@link AsyncTaskExecutor}.
	 *
	 * @return
	 * @see ThreadPoolTaskScheduler
	 */
	@Bean
	@ConditionalOnMissingBean(TaskSchedulerWrapper.class)
	public TaskSchedulerWrapper<ThreadPoolTaskScheduler> vaultTaskScheduler() {

		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(2);
		threadPoolTaskScheduler.setThreadNamePrefix("Spring-Cloud-Vault-");

		return new TaskSchedulerWrapper<>(threadPoolTaskScheduler);
	}

	/**
	 * @return the {@link SessionManager} for Vault session management.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	public SessionManager sessionManager(
			ClientAuthentication clientAuthentication,
			ObjectFactory<TaskSchedulerWrapper<? extends AsyncTaskExecutor>> asyncTaskExecutorFactory) {

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {
			return new LifecycleAwareSessionManager(clientAuthentication,
					asyncTaskExecutorFactory.getObject().getTaskScheduler(),
					vaultClient());
		}

		return new SimpleSessionManager(clientAuthentication);
	}

	@Bean
	@ConditionalOnMissingBean
	public ClientAuthentication clientAuthentication() {

		VaultClient vaultClient = vaultClient();

		switch (vaultProperties.getAuthentication()) {

		case TOKEN:
			Assert.hasText(vaultProperties.getToken(),
					"Token (spring.cloud.vault.token) must not be empty");
			return new TokenAuthentication(vaultProperties.getToken());

		case APPID:
			return appIdAuthentication(vaultProperties, vaultClient);

		case APPROLE:
			return appRoleAuthentication(vaultProperties, vaultClient);

		case CERT:
			return new ClientCertificateAuthentication(vaultClient);

		case AWS_EC2:
			return awsEc2Authentication(vaultProperties, vaultClient);

		case CUBBYHOLE:
			return cubbyholeAuthentication(vaultClient);

		}

		throw new UnsupportedOperationException(String.format(
				"Client authentication %s not supported",
				vaultProperties.getAuthentication()));
	}

	private ClientAuthentication appIdAuthentication(VaultProperties vaultProperties,
			VaultClient vaultClient) {

		VaultProperties.AppIdProperties appId = vaultProperties.getAppId();
		Assert.hasText(appId.getUserId(),
				"UserId (spring.cloud.vault.app-id.user-id) must not be empty");

		AppIdAuthenticationOptions authenticationOptions = AppIdAuthenticationOptions
				.builder().appId(vaultProperties.getApplicationName()) //
				.path(appId.getAppIdPath()) //
				.userIdMechanism(getClientAuthentication(appId)).build();

		return new AppIdAuthentication(authenticationOptions, vaultClient);
	}

	private AppIdUserIdMechanism getClientAuthentication(
			VaultProperties.AppIdProperties appId) {

		try {
			Class<?> userIdClass = ClassUtils.forName(appId.getUserId(), null);
			return (AppIdUserIdMechanism) BeanUtils.instantiateClass(userIdClass);
		}
		catch (ClassNotFoundException ex) {

			switch (appId.getUserId().toUpperCase()) {

			case VaultProperties.AppIdProperties.IP_ADDRESS:
				return new IpAddressUserId();

			case VaultProperties.AppIdProperties.MAC_ADDRESS:

				if (StringUtils.hasText(appId.getNetworkInterface())) {
					try {
						return new MacAddressUserId(Integer.parseInt(appId
								.getNetworkInterface()));
					}
					catch (NumberFormatException e) {
						return new MacAddressUserId(appId.getNetworkInterface());
					}
				}

				return new MacAddressUserId();
			default:
				return new StaticUserId(appId.getUserId());
			}
		}
	}

	private ClientAuthentication appRoleAuthentication(VaultProperties vaultProperties,
			VaultClient vaultClient) {

		VaultProperties.AppRoleProperties appRole = vaultProperties.getAppRole();
		Assert.hasText(appRole.getRoleId(),
				"RoleId (spring.cloud.vault.app-role.role-id) must not be empty");

		AppRoleAuthenticationOptions.AppRoleAuthenticationOptionsBuilder builder = AppRoleAuthenticationOptions
				.builder().path(appRole.getAppRolePath()).roleId(appRole.getRoleId());

		if (StringUtils.hasText(appRole.getSecretId())) {
			builder = builder.secretId(appRole.getSecretId());
		}

		return new AppRoleAuthentication(builder.build(), vaultClient);
	}

	private ClientAuthentication awsEc2Authentication(VaultProperties vaultProperties,
			VaultClient vaultClient) {

		VaultProperties.AwsEc2Properties awsEc2 = vaultProperties.getAwsEc2();

		AwsEc2AuthenticationOptions authenticationOptions = AwsEc2AuthenticationOptions
				.builder().role(awsEc2.getRole()) //
				.path(awsEc2.getAwsEc2Path()) //
				.identityDocumentUri(URI.create(awsEc2.getIdentityDocument())) //
				.build();

		return new AwsEc2Authentication(authenticationOptions, vaultClient,
				vaultClient.getRestTemplate());
	}

	private ClientAuthentication cubbyholeAuthentication(VaultClient vaultClient) {

		Assert.hasText(vaultProperties.getToken(),
				"Initial Token (spring.cloud.vault.token) for Cubbyhole authentication must not be empty");

		CubbyholeAuthenticationOptions options = CubbyholeAuthenticationOptions.builder() //
				.wrapped() //
				.initialToken(VaultToken.of(vaultProperties.getToken())) //
				.build();

		return new CubbyholeAuthentication(options, vaultClient);
	}

	/**
	 * Wrapper to keep {@link TaskScheduler} local to Spring Cloud Vault.
	 * @param <T>
	 */
	public static class TaskSchedulerWrapper<T extends AsyncTaskExecutor & TaskScheduler>
			implements InitializingBean, DisposableBean {

		private final T taskScheduler;

		public TaskSchedulerWrapper(T taskScheduler) {
			this.taskScheduler = taskScheduler;
		}

		T getTaskScheduler() {
			return taskScheduler;
		}

		@Override
		public void destroy() throws Exception {
			if (taskScheduler instanceof DisposableBean) {
				((DisposableBean) taskScheduler).destroy();
			}
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			if (taskScheduler instanceof InitializingBean) {
				((InitializingBean) taskScheduler).afterPropertiesSet();
			}
		}
	}
}
