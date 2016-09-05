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
package org.springframework.cloud.vault.config.aws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.SecureBackendAccessorFactory;
import org.springframework.cloud.vault.VaultSecretBackend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
public class VaultConfigAwsBootstrapConfiguration {

	@Bean
	public SecureBackendAccessorFactory<VaultAwsProperties> secureBackendAccessorFactory() {
		return new AwsSecureBackendAccessorFactory();
	}

	@Bean
	public VaultAwsProperties awsProperties() {
		return new VaultAwsProperties();
	}

	static class AwsSecureBackendAccessorFactory
			implements SecureBackendAccessorFactory<VaultAwsProperties> {

		@Override
		public SecureBackendAccessor createSecureBackendAccessor(
				VaultAwsProperties properties) {
			return forAws(properties);
		}

		@Override
		public boolean supports(VaultSecretBackend secretBackend) {
			return secretBackend instanceof VaultAwsProperties;
		}

		/**
		 * Creates a {@link SecureBackendAccessor} for a secure backend using
		 * {@link VaultAwsProperties}. This accessor transforms Vault's
		 * username/password property names to names provided with
		 * {@link VaultAwsProperties#getAccessKeyProperty()} and
		 * {@link VaultAwsProperties#getSecretKeyProperty()}.
		 *
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecureBackendAccessor}
		 */
		public static SecureBackendAccessor forAws(
				final VaultAwsProperties properties) {
			Assert.notNull(properties, "VaultAwsProperties must not be null");

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
					result.put(properties.getAccessKeyProperty(), input.get("access_key"));
					result.put(properties.getSecretKeyProperty(), input.get("secret_key"));

					return result;
				}
			};
		}
	}
}
