/*
 * Copyright 2019-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.cloud.vault.config.VaultAutoConfiguration.TaskSchedulerWrapper;
import org.springframework.core.env.PropertySource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.LifecycleAwareSessionManagerSupport;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.VaultHttpHeaders;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.env.LeaseAwareVaultPropertySource;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.LeaseErrorListener;
import org.springframework.web.client.RestTemplate;

import static org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;

/**
 * {@link ConfigDataLoader} for Vault for {@link VaultConfigLocation}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class VaultConfigDataLoader implements ConfigDataLoader<VaultConfigLocation> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, VaultConfigLocation location)
			throws IOException, ConfigDataLocationNotFoundException {

		ConfigurableBootstrapContext bootstrap = context.getBootstrapContext();
		VaultProperties vaultProperties = bootstrap.get(VaultProperties.class);
		ImperativeConfiguration configuration = new ImperativeConfiguration(vaultProperties);

		if (vaultProperties.getSession().getLifecycle().isEnabled()
				|| vaultProperties.getConfig().getLifecycle().isEnabled()) {

			bootstrap.registerIfAbsent(TaskSchedulerWrapper.class, ctx -> {

				ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
				threadPoolTaskScheduler.setPoolSize(2);
				threadPoolTaskScheduler.setDaemon(true);
				threadPoolTaskScheduler.setThreadNamePrefix("Spring-Cloud-Vault-");

				threadPoolTaskScheduler.afterPropertiesSet();

				// TODO
				// This is to destroy bootstrap resources
				// otherwise, the bootstrap context is not shut down cleanly
				// this.applicationContext.registerShutdownHook();

				return new TaskSchedulerWrapper(threadPoolTaskScheduler);
			});
		}
		bootstrap.registerIfAbsent(ClientFactoryWrapper.class, ctx -> new ClientFactoryWrapper(
				VaultConfigurationUtil.createClientHttpRequestFactory(vaultProperties)));
		bootstrap.registerIfAbsent(RestTemplateBuilder.class, ctx -> configuration
				.createRestTemplateBuilder(ctx.get(ClientFactoryWrapper.class).getClientHttpRequestFactory()));

		bootstrap.registerIfAbsent(RestTemplateFactory.class,
				ctx -> new DefaultRestTemplateFactory(ctx.get(ClientFactoryWrapper.class).getClientHttpRequestFactory(),
						configuration::createRestTemplateBuilder));

		ClientHttpRequestFactory factory = bootstrap.get(ClientFactoryWrapper.class).getClientHttpRequestFactory();

		RestTemplate externalRestTemplate = new RestTemplate(factory);

		ClientAuthenticationFactory authenticationFactory = new ClientAuthenticationFactory(vaultProperties,
				bootstrap.get(RestTemplateFactory.class).create(), externalRestTemplate);
		ClientAuthentication clientAuthentication = authenticationFactory.createClientAuthentication();

		VaultProperties.AuthenticationMethod authentication = vaultProperties.getAuthentication();

		if (authentication == VaultProperties.AuthenticationMethod.NONE) {
			bootstrap.registerIfAbsent(VaultTemplate.class,
					ctx -> new VaultTemplate(ctx.get(RestTemplateBuilder.class)));
		}
		else {

			bootstrap.registerIfAbsent(SessionManager.class, ctx -> {

				// TODO: Create blocking adapter for Reactive Session Manager, if present.

				VaultProperties.SessionLifecycle lifecycle = vaultProperties.getSession().getLifecycle();

				if (lifecycle.isEnabled()) {
					RestTemplate restTemplate = bootstrap.get(RestTemplateFactory.class).create();
					LifecycleAwareSessionManagerSupport.RefreshTrigger trigger = new LifecycleAwareSessionManagerSupport.FixedTimeoutRefreshTrigger(
							lifecycle.getRefreshBeforeExpiry(), lifecycle.getExpiryThreshold());
					return new LifecycleAwareSessionManager(clientAuthentication,
							ctx.get(TaskSchedulerWrapper.class).getTaskScheduler(), restTemplate, trigger);
				}

				return new SimpleSessionManager(clientAuthentication);
			});

			bootstrap.registerIfAbsent(VaultTemplate.class,
					ctx -> new VaultTemplate(bootstrap.get(RestTemplateBuilder.class),
							bootstrap.get(SessionManager.class)));
		}

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {

			VaultProperties.ConfigLifecycle lifecycle = vaultProperties.getConfig().getLifecycle();

			bootstrap.registerIfAbsent(SecretLeaseContainer.class, ctx -> {
				SecretLeaseContainer container = new SecretLeaseContainer(ctx.get(VaultTemplate.class),
						ctx.get(TaskSchedulerWrapper.class).getTaskScheduler());

				customizeContainer(lifecycle, container);

				// This is to destroy bootstrap resources
				// otherwise, the bootstrap context is not shut down cleanly
				// TODO
				// this.applicationContext.registerShutdownHook();

				try {
					container.afterPropertiesSet();
				}
				catch (Exception e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}
				container.start();

				return container;
			});

			RequestedSecret secret = getRequestedSecret(location.getSecretBackendMetadata());

			if (vaultProperties.isFailFast()) {
				return new ConfigData(Collections.singleton(createLeasingPropertySourceFailFast(
						bootstrap.get(SecretLeaseContainer.class), secret, location.getSecretBackendMetadata())));
			}

			return new ConfigData(Collections.singleton(createLeasingPropertySource(
					bootstrap.get(SecretLeaseContainer.class), secret, location.getSecretBackendMetadata())));
		}
		bootstrap.registerIfAbsent(VaultConfigTemplate.class,
				ctx -> new VaultConfigTemplate(ctx.get(VaultTemplate.class), vaultProperties));
		VaultConfigTemplate configTemplate = bootstrap.get(VaultConfigTemplate.class);
		return new ConfigData(Collections.singletonList(createVaultPropertySource(configTemplate,
				vaultProperties.isFailFast(), location.getSecretBackendMetadata())));
	}

	private PropertySource<?> createVaultPropertySource(VaultConfigOperations configOperations, boolean failFast,
			SecretBackendMetadata accessor) {

		VaultPropertySource vaultPropertySource = new VaultPropertySource(configOperations, failFast, accessor);
		vaultPropertySource.init();
		return vaultPropertySource;
	}

	private PropertySource<?> createLeasingPropertySource(SecretLeaseContainer secretLeaseContainer,
			RequestedSecret secret, SecretBackendMetadata accessor) {

		if (accessor instanceof LeasingSecretBackendMetadata) {
			((LeasingSecretBackendMetadata) accessor).beforeRegistration(secret, secretLeaseContainer);
		}

		LeaseAwareVaultPropertySource propertySource = new LeaseAwareVaultPropertySource(accessor.getName(),
				secretLeaseContainer, secret, accessor.getPropertyTransformer());

		if (accessor instanceof LeasingSecretBackendMetadata) {
			((LeasingSecretBackendMetadata) accessor).afterRegistration(secret, secretLeaseContainer);
		}

		return propertySource;
	}

	private PropertySource<?> createLeasingPropertySourceFailFast(SecretLeaseContainer secretLeaseContainer,
			RequestedSecret secret, SecretBackendMetadata accessor) {

		final AtomicReference<Exception> errorRef = new AtomicReference<>();

		LeaseErrorListener errorListener = (leaseEvent, exception) -> {

			if (leaseEvent.getSource() == secret) {
				errorRef.compareAndSet(null, exception);
			}
		};

		secretLeaseContainer.addErrorListener(errorListener);
		try {
			return createLeasingPropertySource(secretLeaseContainer, secret, accessor);
		}
		finally {
			secretLeaseContainer.removeLeaseErrorListener(errorListener);

			Exception exception = errorRef.get();
			if (exception != null) {
				if (exception instanceof VaultException) {
					throw (VaultException) exception;
				}
				throw new VaultException(
						String.format("Cannot initialize PropertySource for secret at %s", secret.getPath()),
						exception);
			}
		}
	}

	private RequestedSecret getRequestedSecret(SecretBackendMetadata accessor) {

		if (accessor instanceof LeasingSecretBackendMetadata) {

			LeasingSecretBackendMetadata leasingBackend = (LeasingSecretBackendMetadata) accessor;
			return RequestedSecret.from(leasingBackend.getLeaseMode(), accessor.getPath());
		}

		if (accessor instanceof KeyValueSecretBackendMetadata) {
			return RequestedSecret.rotating(accessor.getPath());
		}

		return RequestedSecret.renewable(accessor.getPath());
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
		}
	}

	static class ImperativeConfiguration {

		private final VaultProperties vaultProperties;

		private final VaultEndpointProvider endpointProvider;

		ImperativeConfiguration(VaultProperties vaultProperties) {
			this.vaultProperties = vaultProperties;
			this.endpointProvider = SimpleVaultEndpointProvider
					.of(VaultConfigurationUtil.createVaultEndpoint(vaultProperties));
		}

		public RestTemplateBuilder createRestTemplateBuilder(ClientHttpRequestFactory requestFactory) {

			RestTemplateBuilder builder = RestTemplateBuilder.builder().requestFactory(requestFactory)
					.endpointProvider(this.endpointProvider);

			if (StringUtils.hasText(this.vaultProperties.getNamespace())) {
				builder.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, this.vaultProperties.getNamespace());
			}

			return builder;
		}

	}

	// TODO
	static class ReactiveConfiguration {

		private final VaultProperties vaultProperties;

		private final VaultEndpointProvider endpointProvider;

		ReactiveConfiguration(VaultProperties vaultProperties) {
			this.vaultProperties = vaultProperties;
			this.endpointProvider = SimpleVaultEndpointProvider
					.of(VaultConfigurationUtil.createVaultEndpoint(vaultProperties));
		}

		public WebClientBuilder createRestTemplateBuilder(ClientHttpConnector connector) {

			WebClientBuilder builder = WebClientBuilder.builder().httpConnector(connector)
					.endpointProvider(this.endpointProvider);

			if (StringUtils.hasText(this.vaultProperties.getNamespace())) {
				builder.defaultHeader(VaultHttpHeaders.VAULT_NAMESPACE, this.vaultProperties.getNamespace());
			}

			return builder;
		}

	}

}
