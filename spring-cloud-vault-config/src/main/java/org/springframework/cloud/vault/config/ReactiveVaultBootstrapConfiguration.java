/*
 * Copyright 2017 the original author or authors.
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
import java.time.Duration;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.AuthenticationStepsOperator;
import org.springframework.vault.authentication.CachingVaultTokenSupplier;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.ReactiveVaultClients;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.ClientHttpConnectorFactory;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive Spring Vault support.
 *
 * @author Mark Paluch
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnClass({ Flux.class, WebClient.class, ReactiveVaultOperations.class })
@EnableConfigurationProperties({ VaultProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ReactiveVaultBootstrapConfiguration {

	private final VaultProperties vaultProperties;

	private final VaultEndpoint vaultEndpoint;

	private final ClientHttpConnector clientHttpConnector;

	public ReactiveVaultBootstrapConfiguration(VaultProperties vaultProperties) {

		this.vaultProperties = vaultProperties;
		this.vaultEndpoint = getVaultEndpoint(vaultProperties);
		this.clientHttpConnector = createConnector(this.vaultProperties);
	}

	private static VaultEndpoint getVaultEndpoint(VaultProperties vaultProperties) {

		if (StringUtils.hasText(vaultProperties.getUri())) {
			return VaultEndpoint.from(URI.create(vaultProperties.getUri()));
		}

		VaultEndpoint vaultEndpoint = new VaultEndpoint();
		vaultEndpoint.setHost(vaultProperties.getHost());
		vaultEndpoint.setPort(vaultProperties.getPort());
		vaultEndpoint.setScheme(vaultProperties.getScheme());

		return vaultEndpoint;
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

		VaultProperties.Ssl ssl = vaultProperties.getSsl();
		SslConfiguration sslConfiguration;
		if (ssl != null) {

			KeyStoreConfiguration keyStore = KeyStoreConfiguration.EMPTY;
			KeyStoreConfiguration trustStore = KeyStoreConfiguration.EMPTY;

			if (ssl.getKeyStore() != null) {
				keyStore = new KeyStoreConfiguration(ssl.getKeyStore(),
						ssl.getKeyStorePassword() != null ? ssl.getKeyStorePassword()
								.toCharArray() : null, null);
			}

			if (ssl.getTrustStore() != null) {
				trustStore = new KeyStoreConfiguration(ssl.getTrustStore(),
						ssl.getTrustStorePassword() != null ? ssl.getTrustStorePassword()
								.toCharArray() : null, null);
			}

			sslConfiguration = new SslConfiguration(keyStore, trustStore);
		}
		else {
			sslConfiguration = SslConfiguration.NONE;
		}

		return ClientHttpConnectorFactory.create(clientOptions, sslConfiguration);
	}

	/**
	 * Creates a {@link ReactiveVaultTemplate}.
	 *
	 * @return
	 * @see #vaultTokenSupplier(AuthenticationStepsFactory)
	 */
	@Bean
	@ConditionalOnMissingBean
	public ReactiveVaultTemplate reactiveVaultTemplate(VaultTokenSupplier tokenSupplier) {
		return new ReactiveVaultTemplate(vaultEndpoint, clientHttpConnector,
				tokenSupplier);
	}

	/**
	 * @return the {@link VaultTokenSupplier} for reactive Vault session management.
	 * @see AuthenticationStepsFactory
	 */
	@Bean
	@ConditionalOnMissingBean(VaultTokenSupplier.class)
	public VaultTokenSupplier vaultTokenSupplier(AuthenticationStepsFactory factory) {

		WebClient webClient = ReactiveVaultClients.createWebClient(this.vaultEndpoint,
				this.clientHttpConnector);
		AuthenticationStepsOperator operator = new AuthenticationStepsOperator(
				factory.getAuthenticationSteps(), webClient);

		return CachingVaultTokenSupplier.of(operator);
	}
}
