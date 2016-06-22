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
package org.springframework.cloud.vault.config.rabbitmq;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.VaultSecretBackend;
import org.springframework.cloud.vault.config.SecureBackendAccessorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
public class VaultConfigRabbitMqBootstrapConfiguration {

	@Bean
	public SecureBackendAccessorFactory<VaultRabbitMqProperties> secureBackendAccessorFactory() {
		return new RabbitMqSecureBackendAccessorFactory();
	}

	@Bean
	public VaultRabbitMqProperties rabbitMqProperties() {
		return new VaultRabbitMqProperties();
	}

	static class RabbitMqSecureBackendAccessorFactory
			implements SecureBackendAccessorFactory<VaultRabbitMqProperties> {

		@Override
		public SecureBackendAccessor createSecureBackendAccessor(
				VaultRabbitMqProperties properties) {
			return forRabbitMq(properties);
		}

		@Override
		public boolean supports(VaultSecretBackend secretBackend) {
			return secretBackend instanceof VaultRabbitMqProperties;
		}

		/**
		 * Creates a {@link SecureBackendAccessor} for a secure backend using
		 * {@link VaultRabbitMqProperties}. This accessor transforms Vault's
		 * username/password property names to names provided with
		 * {@link VaultRabbitMqProperties#getUsernameProperty()} and
		 * {@link VaultRabbitMqProperties#getPasswordProperty()}.
		 *
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecureBackendAccessor}
		 */
		public static SecureBackendAccessor forRabbitMq(
				final VaultRabbitMqProperties properties) {
			Assert.notNull(properties, "DatabaseSecretProperties must not be null");

			return new SecureBackendAccessor() {

				@Override
				public Map<String, String> variables() {

					Map<String, String> variables = new HashMap<>();
					variables.put("backend", properties.getBackend());
					variables.put("key", String.format("creds/%s", properties.getRole()));
					return variables;
				}

				@Override
				public String getName() {
					return String.format("%s with Role %s", properties.getBackend(),
							properties.getRole());
				}

				@Override
				public Map<String, String> transformProperties(
						Map<String, String> input) {

					Map<String, String> result = new HashMap();
					result.put(properties.getUsernameProperty(), input.get("username"));
					result.put(properties.getPasswordProperty(), input.get("password"));

					return result;
				}
			};
		}
	}
}
