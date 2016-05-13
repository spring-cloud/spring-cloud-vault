/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
public class VaultBootstrapConfiguration {

	@Bean
	public VaultClient vaultClient(ApplicationContext applicationContext) {

		VaultClient vaultClient = new VaultClient(vaultProperties());

		Map<String, AppIdUserIdMechanism> appIdUserIdMechanisms = applicationContext
				.getBeansOfType(AppIdUserIdMechanism.class);
		if (!appIdUserIdMechanisms.isEmpty()) {
			vaultClient.setAppIdUserIdMechanism(
					appIdUserIdMechanisms.values().iterator().next());
		}

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

	@Bean
	public VaultPropertySourceLocator vaultPropertySourceLocator(
			ApplicationContext applicationContext) {
		return new VaultPropertySourceLocator(vaultClient(applicationContext),
				vaultProperties());
	}
}
