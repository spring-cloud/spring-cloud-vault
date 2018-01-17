/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.vault.config.consul;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.config.PropertyNameTransformer;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Bootstrap configuration providing support for the Consul secret backend.
 *
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties(VaultConsulProperties.class)
public class VaultConfigConsulBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConsulSecretBackendMetadataFactory consulSecretBackendAccessorFactory() {
		return new ConsulSecretBackendMetadataFactory();
	}

	/**
	 * {@link SecretBackendMetadataFactory} for Consul integration using
	 * {@link VaultConsulProperties}.
	 */
	public static class ConsulSecretBackendMetadataFactory
			implements SecretBackendMetadataFactory<VaultConsulProperties> {

		@Override
		public SecretBackendMetadata createMetadata(
				VaultConsulProperties backendDescriptor) {
			return forConsul(backendDescriptor);
		}

		@Override
		public boolean supports(VaultSecretBackendDescriptor backendDescriptor) {
			return backendDescriptor instanceof VaultConsulProperties;
		}

		/**
		 * Creates a {@link SecretBackendMetadata} for a secret backend using
		 * {@link VaultConsulProperties}. This accessor transforms Vault's token property
		 * names to names provided with {@link VaultConsulProperties#getTokenProperty()}.
		 *
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecretBackendMetadata}
		 */
		static SecretBackendMetadata forConsul(
				final VaultConsulProperties properties) {

			Assert.notNull(properties, "VaultConsulProperties must not be null");

			final PropertyNameTransformer transformer = new PropertyNameTransformer();
			transformer.addKeyTransformation("token", properties.getTokenProperty());

			return new SecretBackendMetadata() {

				@Override
				public String getName() {
					return String.format("%s with Role %s", properties.getBackend(),
							properties.getRole());
				}

				@Override
				public String getPath() {
					return String.format("%s/creds/%s", properties.getBackend(),
							properties.getRole());
				}

				@Override
				public Map<String, String> getVariables() {

					Map<String, String> variables = new HashMap<>();

					variables.put("backend", properties.getBackend());
					variables.put("key", String.format("creds/%s", properties.getRole()));

					return variables;
				}

				@Override
				public PropertyTransformer getPropertyTransformer() {
					return transformer;
				}
			};
		}
	}
}
