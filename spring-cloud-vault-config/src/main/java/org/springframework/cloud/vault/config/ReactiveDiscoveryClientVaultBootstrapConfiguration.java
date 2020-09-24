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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link org.springframework.cloud.bootstrap.BootstrapConfiguration} providing a
 * {@link VaultEndpointProvider} using {@link DiscoveryClient}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.cloud.vault.discovery.enabled")
@ConditionalOnExpression("${spring.cloud.vault.reactive.enabled:true}")
@ConditionalOnClass({ Flux.class, WebClient.class, ReactiveVaultOperations.class, ReactiveDiscoveryClient.class })
@EnableConfigurationProperties(VaultProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 5)
@Import(UtilAutoConfiguration.class)
public class ReactiveDiscoveryClientVaultBootstrapConfiguration {

	private final VaultProperties vaultProperties;

	private final VaultConfiguration configuration;

	public ReactiveDiscoveryClientVaultBootstrapConfiguration(VaultProperties vaultProperties) {
		this.vaultProperties = vaultProperties;
		this.configuration = new VaultConfiguration(vaultProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
	public ReactiveVaultEndpointProvider reactiveVaultEndpointProvider(
			ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClients,
			ObjectProvider<VaultEndpointProvider> endpointProviders) {

		ReactiveDiscoveryClient reactiveDiscoveryClient = reactiveDiscoveryClients.getIfAvailable();

		if (reactiveDiscoveryClient != null) {
			ReacvtiveDiscoveryClientVaultServiceInstanceProvider instanceProvider = new ReacvtiveDiscoveryClientVaultServiceInstanceProvider(
					reactiveDiscoveryClient);

			return () -> Mono.defer(() -> {

				String serviceId = this.vaultProperties.getDiscovery().getServiceId();

				return instanceProvider.getVaultServerInstance(serviceId).map(this.configuration::createVaultEndpoint);
			});
		}

		VaultEndpointProvider endpointProvider = endpointProviders.getObject();

		return () -> Mono.fromSupplier(endpointProvider::getVaultEndpoint).subscribeOn(Schedulers.boundedElastic());
	}

}
