/*
 * Copyright 2018-2019 the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DiscoveryClientVaultBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class DiscoveryClientVaultBootstrapConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					DiscoveryClientVaultBootstrapConfiguration.class,
					VaultBootstrapConfiguration.class));

	@Test
	public void shouldRegisterDefaultBeans() {

		this.contextRunner.withUserConfiguration(DiscoveryConfiguration.class)
				.withPropertyValues("spring.cloud.vault.token=foo",
						"spring.cloud.vault.discovery.enabled=true")
				.run(context -> {

					assertThat(context.getBean(VaultServiceInstanceProvider.class))
							.isInstanceOf(
									DiscoveryClientVaultServiceInstanceProvider.class);

					VaultEndpointProvider endpointProvider = context
							.getBean(VaultEndpointProvider.class);
					VaultEndpoint vaultEndpoint = endpointProvider.getVaultEndpoint();
					assertThat(vaultEndpoint.getPort()).isEqualTo(1234);
				});
	}

	@Test
	public void shouldNotRegisterBeansIfDiscoveryDisabled() {

		this.contextRunner.withUserConfiguration(DiscoveryConfiguration.class)
				.withPropertyValues("spring.cloud.vault.token=foo",
						"spring.cloud.vault.discovery.enabled=false")
				.run(context -> {

					assertThat(context
							.getBeanNamesForType(VaultServiceInstanceProvider.class))
									.isEmpty();
				});
	}

	@Test
	public void shouldNotRegisterBeansIfVaultDisabled() {

		this.contextRunner.withUserConfiguration(DiscoveryConfiguration.class)
				.withPropertyValues("spring.cloud.vault.token=foo",
						"spring.cloud.vault.enabled=false")
				.run(context -> {

					assertThat(context
							.getBeanNamesForType(VaultServiceInstanceProvider.class))
									.isEmpty();
				});

	}

	@Configuration
	static class DiscoveryConfiguration {

		@Bean
		DiscoveryClient discoveryClient() {

			DiscoveryClient mock = Mockito.mock(DiscoveryClient.class);
			when(mock.getInstances(anyString())).thenReturn(Collections.singletonList(
					new SimpleServiceInstance(URI.create("https://foo:1234"))));

			return mock;
		}

	}

	static class SimpleServiceInstance implements ServiceInstance {

		private URI uri;

		private String host;

		private int port;

		private boolean secure;

		private Map<String, String> metadata = new LinkedHashMap<>();

		private String serviceId;

		SimpleServiceInstance(URI uri) {
			this.setUri(uri);
		}

		void setUri(URI uri) {
			this.uri = uri;
			this.host = this.uri.getHost();
			this.port = this.uri.getPort();
			String scheme = this.uri.getScheme();
			if ("https".equals(scheme)) {
				this.secure = true;
			}

		}

		public URI getUri() {
			return this.uri;
		}

		public String getHost() {
			return this.host;
		}

		public int getPort() {
			return this.port;
		}

		public boolean isSecure() {
			return this.secure;
		}

		public Map<String, String> getMetadata() {
			return this.metadata;
		}

		public String getServiceId() {
			return this.serviceId;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public void setMetadata(Map<String, String> metadata) {
			this.metadata = metadata;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

	}

}
