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
package org.springframework.cloud.vault.config;

import static org.springframework.cloud.vault.VaultBootstrapConfiguration.*;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.AppIdUserIdMechanism;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.VaultBootstrapConfiguration;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultSecretBackend;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnBean(VaultBootstrapConfiguration.class)
public class VaultConfigBootstrapConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private Collection<VaultSecretBackend> vaultSecretBackends;
	private Collection<SecureBackendAccessorFactory<? super VaultSecretBackend>> factories;

	@Bean
	public VaultGenericBackendProperties vaultGenericBackendProperties() {
		return new VaultGenericBackendProperties();
	}

	@Bean
	public VaultPropertySourceLocator vaultPropertySourceLocator(VaultClient vaultClient,
			VaultProperties vaultProperties,
			VaultGenericBackendProperties vaultGenericBackendProperties,
			ClientFactoryWrapper clientFactoryWrapper) {

		Collection<SecureBackendAccessor> backendAccessors = SecureBackendFactories
				.createBackendAcessors(vaultSecretBackends, factories);

		ClientAuthentication clientAuthentication = clientAuthentication(
				applicationContext, clientFactoryWrapper, vaultProperties);

		return new VaultPropertySourceLocator(vaultClient, clientAuthentication,
				vaultProperties, vaultGenericBackendProperties, backendAccessors);
	}

	private ClientAuthentication clientAuthentication(
			ApplicationContext applicationContext,
			ClientFactoryWrapper clientFactoryWrapper, VaultProperties vaultProperties) {

		RestTemplate restTemplate = new RestTemplate(
				clientFactoryWrapper.getClientHttpRequestFactory());
		ClientAuthentication clientAuthentication;

		if (vaultProperties
				.getAuthentication() == VaultProperties.AuthenticationMethod.TOKEN) {
			clientAuthentication = ClientAuthentication.token(vaultProperties);
		}
		else if (vaultProperties
				.getAuthentication() == VaultProperties.AuthenticationMethod.APPID) {

			Map<String, AppIdUserIdMechanism> appIdUserIdMechanisms = applicationContext
					.getBeansOfType(AppIdUserIdMechanism.class);
			if (!appIdUserIdMechanisms.isEmpty()) {
				clientAuthentication = ClientAuthentication.appId(vaultProperties,
						restTemplate, appIdUserIdMechanisms.values().iterator().next());
			}
			else {
				clientAuthentication = ClientAuthentication.appId(vaultProperties,
						restTemplate);
			}
		}
		else {
			clientAuthentication = ClientAuthentication.create(vaultProperties,
					restTemplate);
		}

		return clientAuthentication;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	private void postConstruct() {

		this.vaultSecretBackends = applicationContext
				.getBeansOfType(VaultSecretBackend.class).values();
		this.factories = (Collection) applicationContext
				.getBeansOfType(SecureBackendAccessorFactory.class).values();

	}
}
