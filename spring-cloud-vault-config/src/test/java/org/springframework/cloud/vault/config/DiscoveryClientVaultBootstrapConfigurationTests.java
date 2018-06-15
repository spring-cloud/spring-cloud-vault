/*
 * Copyright 2018 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DiscoveryClientVaultBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class DiscoveryClientVaultBootstrapConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void shouldRegisterDefaultBeans() {

		load(DiscoveryConfiguration.class, "spring.cloud.vault.token=foo",
				"spring.cloud.vault.discovery.enabled=true");

		assertThat(context.getBean(VaultServiceInstanceProvider.class)).isInstanceOf(
				DiscoveryClientVaultServiceInstanceProvider.class);

		VaultEndpointProvider endpointProvider = context
				.getBean(VaultEndpointProvider.class);
		VaultEndpoint vaultEndpoint = endpointProvider.getVaultEndpoint();
		assertThat(vaultEndpoint.getPort()).isEqualTo(1234);
	}

	@Test
	public void shouldNotRegisterBeansIfDiscoveryDisabled() {

		load(DiscoveryConfiguration.class, "spring.cloud.vault.token=foo",
				"spring.cloud.vault.discovery.enabled=false");

		assertThat(context.getBeanNamesForType(VaultServiceInstanceProvider.class))
				.isEmpty();
	}

	@Test
	public void shouldNotRegisterBeansIfVaultDisabled() {

		load(DiscoveryConfiguration.class, "spring.cloud.vault.token=foo",
				"spring.cloud.vault.enabled=false");

		assertThat(context.getBeanNamesForType(VaultServiceInstanceProvider.class))
				.isEmpty();
	}

	private void load(Class<?> config, String... environment) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		TestPropertyValues.of(environment).applyTo(ctx);

		ctx.register(config);
		ctx.register(DiscoveryClientVaultBootstrapConfiguration.class);
		ctx.register(VaultBootstrapConfiguration.class);
		ctx.refresh();

		this.context = ctx;
	}

	@Configuration
	static class DiscoveryConfiguration {

		@Bean
		DiscoveryClient discoveryClient() {

			DiscoveryClient mock = Mockito.mock(DiscoveryClient.class);
			when(mock.getInstances(anyString())).thenReturn(
					Collections.singletonList(new SimpleServiceInstance(URI
							.create("https://foo:1234"))));

			return mock;
		}
	}

	@Data
	static class SimpleServiceInstance implements ServiceInstance {
		private URI uri;
		private String host;
		private int port;
		private boolean secure;
		private Map<String, String> metadata = new LinkedHashMap<>();
		private String serviceId;

		public SimpleServiceInstance(URI uri) {
			this.setUri(uri);
		}

		public void setUri(URI uri) {
			this.uri = uri;
			this.host = this.uri.getHost();
			this.port = this.uri.getPort();
			String scheme = this.uri.getScheme();
			if ("https".equals(scheme)) {
				this.secure = true;
			}

		}
	}

}
