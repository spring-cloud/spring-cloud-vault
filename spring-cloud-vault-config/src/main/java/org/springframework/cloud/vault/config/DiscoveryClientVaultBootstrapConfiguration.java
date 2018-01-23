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

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;

/**
 * {@link org.springframework.cloud.bootstrap.BootstrapConfiguration} providing a
 * {@link VaultEndpointProvider} using {@link DiscoveryClient}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.vault.discovery.enabled")
@EnableConfigurationProperties(VaultProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 2)
@EnableDiscoveryClient
@Import(UtilAutoConfiguration.class)
public class DiscoveryClientVaultBootstrapConfiguration {

	private final VaultProperties vaultProperties;

	public DiscoveryClientVaultBootstrapConfiguration(VaultProperties vaultProperties) {
		this.vaultProperties = vaultProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
	public VaultServiceInstanceProvider vaultServerInstanceProvider(
			DiscoveryClient discoveryClient) {
		return new DiscoveryClientVaultServiceInstanceProvider(discoveryClient);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
	public VaultEndpointProvider vaultEndpointProvider(
			VaultServiceInstanceProvider instanceProvider) {

		final String serviceId = this.vaultProperties.getDiscovery().getServiceId();

		final String fallbackScheme;

		if (StringUtils.hasText(this.vaultProperties.getUri())) {
			fallbackScheme = URI.create(this.vaultProperties.getUri()).getScheme();
		}
		else {
			fallbackScheme = this.vaultProperties.getScheme();
		}

		ServiceInstance server = instanceProvider.getVaultServerInstance(serviceId);

		final VaultEndpoint vaultEndpoint = VaultEndpoint.create(server.getHost(),
				server.getPort());

		if (server.getMetadata().containsKey("scheme")) {
			vaultEndpoint.setScheme(server.getMetadata().get("scheme"));
		}
		else {
			vaultEndpoint.setScheme(server.isSecure() ? "https" : fallbackScheme);
		}

		return () -> vaultEndpoint;
	}
}
