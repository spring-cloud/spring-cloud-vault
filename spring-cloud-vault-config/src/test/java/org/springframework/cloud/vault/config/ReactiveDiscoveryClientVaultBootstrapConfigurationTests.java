/*
 * Copyright 2018-present the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.ReactiveVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ReactiveDiscoveryClientVaultBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class ReactiveDiscoveryClientVaultBootstrapConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
		.of(ReactiveDiscoveryClientVaultBootstrapConfiguration.class, VaultBootstrapConfiguration.class));

	@Test
	public void shouldRegisterDefaultBeans() {

		this.contextRunner.withUserConfiguration(ReactiveDiscoveryConfiguration.class)
			.withPropertyValues("spring.cloud.vault.token=foo", "spring.cloud.vault.discovery.enabled=true",
					"spring.cloud.bootstrap.enabled=true")
			.run(context -> {

				assertThat(context).hasSingleBean(ReactiveVaultEndpointProvider.class);

				ReactiveVaultEndpointProvider endpointProvider = context.getBean(ReactiveVaultEndpointProvider.class);

				endpointProvider.getVaultEndpoint().as(StepVerifier::create).assertNext(actual -> {
					assertThat(actual.getPort()).isEqualTo(1234);
				}).verifyComplete();
			});
	}

	@Test
	public void shouldRegisterVaultEndpointAdapterBean() {

		this.contextRunner.withUserConfiguration(BridgedDiscoveryConfiguration.class)
			.withPropertyValues("spring.cloud.vault.token=foo", "spring.cloud.vault.discovery.enabled=true",
					"spring.cloud.bootstrap.enabled=true")
			.run(context -> {

				assertThat(context).hasSingleBean(ReactiveVaultEndpointProvider.class);

				ReactiveVaultEndpointProvider endpointProvider = context.getBean(ReactiveVaultEndpointProvider.class);

				endpointProvider.getVaultEndpoint().as(StepVerifier::create).assertNext(actual -> {
					assertThat(actual.getPort()).isEqualTo(1234);
				}).verifyComplete();
			});
	}

	@Test
	public void shouldNotRegisterBeansIfDiscoveryDisabled() {

		this.contextRunner.withUserConfiguration(ReactiveDiscoveryConfiguration.class)
			.withPropertyValues("spring.cloud.vault.token=foo", "spring.cloud.vault.discovery.enabled=false",
					"spring.cloud.bootstrap.enabled=true")
			.run(context -> {

				assertThat(context.getBeanNamesForType(ReactiveVaultEndpointProvider.class)).isEmpty();
			});
	}

	@Test
	public void shouldNotRegisterBeansIfVaultDisabled() {

		this.contextRunner.withUserConfiguration(ReactiveDiscoveryConfiguration.class)
			.withPropertyValues("spring.cloud.vault.token=foo", "spring.cloud.vault.enabled=false",
					"spring.cloud.bootstrap.enabled=true")
			.run(context -> {

				assertThat(context.getBeanNamesForType(ReactiveVaultEndpointProvider.class)).isEmpty();
			});

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveDiscoveryConfiguration {

		@Bean
		ReactiveDiscoveryClient reactiveDiscoveryClient() {

			ReactiveDiscoveryClient mock = Mockito.mock(ReactiveDiscoveryClient.class);
			when(mock.getInstances(anyString()))
				.thenReturn(Flux.just(new SimpleServiceInstance(URI.create("https://foo:1234"))));

			return mock;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BridgedDiscoveryConfiguration {

		@Bean
		VaultEndpointProvider vaultEndpointProvider() {

			VaultEndpointProvider mock = Mockito.mock(VaultEndpointProvider.class);
			VaultEndpoint vaultEndpoint = VaultEndpoint.create("foo", 1234);

			when(mock.getVaultEndpoint()).thenReturn(vaultEndpoint);

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
