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

package org.springframework.cloud.vault;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
public class VaultBootstrapConfiguration {

	@Bean
	public ClientFactoryWrapper clientHttpRequestFactoryWrapper() {
		return new ClientFactoryWrapper(
				ClientHttpRequestFactoryFactory.create(vaultProperties()));
	}

	@Bean
	public VaultClient vaultClient(ApplicationContext applicationContext) {

		RestTemplate restTemplate = new RestTemplate(
				clientHttpRequestFactoryWrapper().getClientHttpRequestFactory());

		VaultClient vaultClient = new VaultClient();
		vaultClient.setRestTemplate(restTemplate);

		return vaultClient;
	}

	@Bean
	public VaultProperties vaultProperties() {
		return new VaultProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.cloud.vault", name = "authentication", havingValue = "APPID")
	public AppIdUserIdMechanism appIdUserIdMechanism(VaultProperties vaultProperties) {

		String userId = vaultProperties.getAppId().getUserId();
		Assert.hasText(userId,
				"UserId (spring.cloud.vault.app-id.user-id) must not be empty.");

		try {
			Class<?> userIdClass = ClassUtils.forName(userId, null);
			return (AppIdUserIdMechanism) BeanUtils.instantiateClass(userIdClass);
		}
		catch (ClassNotFoundException ex) {

			switch (userId.toUpperCase()) {
			case VaultProperties.AppIdProperties.IP_ADDRESS:
				return new IpAddressUserId();
			case VaultProperties.AppIdProperties.MAC_ADDRESS:
				return new MacAddressUserId(vaultProperties);
			default:
				return new StaticUserId(vaultProperties);
			}
		}
	}

	/**
	 * Wrapper for {@link ClientHttpRequestFactory} to not expose the bean globally.
	 */
	public static class ClientFactoryWrapper implements InitializingBean, DisposableBean {

		private final ClientHttpRequestFactory clientHttpRequestFactory;

		public ClientFactoryWrapper(ClientHttpRequestFactory clientHttpRequestFactory) {
			this.clientHttpRequestFactory = clientHttpRequestFactory;
		}

		@Override
		public void destroy() throws Exception {
			if (clientHttpRequestFactory instanceof DisposableBean) {
				((DisposableBean) clientHttpRequestFactory).destroy();
			}
		}

		@Override
		public void afterPropertiesSet() throws Exception {

			if (clientHttpRequestFactory instanceof InitializingBean) {
				((InitializingBean) clientHttpRequestFactory).afterPropertiesSet();
			}
		}

		public ClientHttpRequestFactory getClientHttpRequestFactory() {
			return clientHttpRequestFactory;
		}
	}
}
