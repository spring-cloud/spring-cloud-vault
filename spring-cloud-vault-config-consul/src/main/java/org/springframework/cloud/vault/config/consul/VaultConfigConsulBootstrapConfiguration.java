/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.vault.config.consul;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.vault.config.PropertyNameTransformer;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Bootstrap configuration providing support for the Consul secret backend.
 *
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VaultConsulProperties.class)
public class VaultConfigConsulBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConsulSecretBackendMetadataFactory consulSecretBackendAccessorFactory(ConfigurationPropertiesRebinder rebinder) {
		return new ConsulSecretBackendMetadataFactory(rebinder);
	}

	/**
	 * {@link SecretBackendMetadataFactory} for Consul integration using
	 * {@link VaultConsulProperties}.
	 */
	public static class ConsulSecretBackendMetadataFactory
			implements SecretBackendMetadataFactory<VaultConsulProperties> {

		private final ConfigurationPropertiesRebinder rebinder;

		public ConsulSecretBackendMetadataFactory(ConfigurationPropertiesRebinder rebinder) {
			this.rebinder = rebinder;
		}

		/**
		 * Creates a {@link SecretBackendMetadata} for a secret backend using
		 * {@link VaultConsulProperties}. This accessor transforms Vault's token property
		 * names to names provided with {@link VaultConsulProperties#getTokenProperty()}.
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecretBackendMetadata}
		 */
		SecretBackendMetadata forConsul(VaultConsulProperties properties) {

			Assert.notNull(properties, "VaultConsulProperties must not be null");

			PropertyNameTransformer transformer = new PropertyNameTransformer();
			transformer.addKeyTransformation("token", properties.getTokenProperty());

			return new ConsulBackendMetadata(properties, transformer, this.rebinder);
		}

		@Override
		public SecretBackendMetadata createMetadata(
				VaultConsulProperties backendDescriptor) {
			return forConsul(backendDescriptor);
		}

		@Override
		public boolean supports(VaultSecretBackendDescriptor backendDescriptor) {
			return backendDescriptor instanceof VaultConsulProperties;
		}

	}

}
