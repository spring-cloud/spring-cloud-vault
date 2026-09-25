/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.vault.config.ldap;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Bootstrap configuration providing support for the LDAP secret engine.
 *
 * @author Drew Mullen
 * @since 5.0.1
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VaultLdapProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE - 15)
public class VaultConfigLdapBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public LdapSecretBackendMetadataFactory ldapSecretBackendMetadataFactory() {
		return new LdapSecretBackendMetadataFactory();
	}

	/**
	 * {@link SecretBackendMetadataFactory} for LDAP integration using
	 * {@link VaultLdapProperties}.
	 */
	public static class LdapSecretBackendMetadataFactory implements SecretBackendMetadataFactory<VaultLdapProperties> {

		/**
		 * Creates a {@link SecretBackendMetadata} for the LDAP secret engine using
		 * {@link VaultLdapProperties}. This accessor transforms Vault's username/password
		 * property names to names provided with
		 * {@link VaultLdapProperties#getUsernameProperty()} and
		 * {@link VaultLdapProperties#getPasswordProperty()}.
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecretBackendMetadata}
		 */
		static SecretBackendMetadata forLdap(final VaultLdapProperties properties) {

			Assert.notNull(properties, "VaultLdapProperties must not be null");

			PropertyNameTransformer transformer = new PropertyNameTransformer();
			transformer.addKeyTransformation("username", properties.getUsernameProperty());
			transformer.addKeyTransformation("password", properties.getPasswordProperty());

			return new SecretBackendMetadata() {

				private final String credPath = properties.isStaticRole() ? "static-cred" : "creds";

				@Override
				public String getName() {
					return String.format("%s with Role %s", properties.getBackend(), properties.getRole());
				}

				@Override
				public String getPath() {
					return String.format("%s/%s/%s", properties.getBackend(), this.credPath, properties.getRole());
				}

				@Override
				public PropertyTransformer getPropertyTransformer() {
					return transformer;
				}

				@Override
				public Map<String, String> getVariables() {

					Map<String, String> variables = new HashMap<>();
					variables.put("backend", properties.getBackend());
					variables.put("key", String.format("%s/%s", this.credPath, properties.getRole()));
					return variables;
				}
			};
		}

		@Override
		public SecretBackendMetadata createMetadata(VaultLdapProperties backendDescriptor) {
			return forLdap(backendDescriptor);
		}

		@Override
		public boolean supports(VaultSecretBackendDescriptor backendDescriptor) {
			return backendDescriptor instanceof VaultLdapProperties;
		}

	}

}
