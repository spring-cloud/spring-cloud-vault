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

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link BootstrapConfiguration Bootstrap-configuration} for reactive Spring Vault
 * support.
 * <p>
 * This auto-configuration only supports static endpoints without
 * {@link org.springframework.vault.client.VaultEndpointProvider} support as endpoint
 * providers could be potentially blocking implementations.
 *
 * @author Mark Paluch
 * @since 2.0.0
 * @deprecated since 3.0, use {@link VaultReactiveAutoConfiguration} through
 * {@code @EnableAutoConfiguration}.
 */
@Deprecated
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnExpression("${spring.cloud.vault.reactive.enabled:true}")
@ConditionalOnClass({ Flux.class, WebClient.class, ReactiveVaultOperations.class, HttpClient.class })
@EnableConfigurationProperties({ VaultProperties.class })
public class VaultReactiveBootstrapConfiguration extends VaultReactiveAutoConfiguration {

	public VaultReactiveBootstrapConfiguration(VaultProperties vaultProperties,
			ObjectProvider<ReactiveVaultEndpointProvider> reactiveEndpointProvider,
			ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<WebClientCustomizer>> webClientCustomizers) {
		super(vaultProperties, reactiveEndpointProvider, endpointProvider, webClientCustomizers);
	}

}
