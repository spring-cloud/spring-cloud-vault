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

package org.springframework.cloud.vault.config.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.config.LeasingSecretBackendMetadata;
import org.springframework.cloud.vault.config.PropertyNameTransformer;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Bootstrap configuration providing support for the AWS secret backend.
 *
 * @author Mark Paluch
 * @author Kris Iyer
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VaultAwsProperties.class)
public class VaultConfigAwsBootstrapConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AwsSecretBackendMetadataFactory awsSecretBackendMetadataFactory() {
		return new AwsSecretBackendMetadataFactory();
	}

	/**
	 * {@link SecretBackendMetadataFactory} for AWS integration using
	 * {@link VaultAwsProperties}.
	 */
	public static class AwsSecretBackendMetadataFactory implements SecretBackendMetadataFactory<VaultAwsProperties> {

		/**
		 * Creates {@link SecretBackendMetadata} for a secret backend using
		 * {@link VaultAwsProperties}. This accessor transforms Vault's username/password
		 * property names to names provided with
		 * {@link VaultAwsProperties#getAccessKeyProperty()} and
		 * {@link VaultAwsProperties#getSecretKeyProperty()}.
		 * {@link VaultAwsProperties#getSessionTokenKeyProperty()}.
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecretBackendMetadata}
		 */
		static SecretBackendMetadata forAws(final VaultAwsProperties properties) {

			Assert.notNull(properties, "VaultAwsProperties must not be null");

			PropertyNameTransformer transformer = new PropertyNameTransformer();
			transformer.addKeyTransformation("access_key", properties.getAccessKeyProperty());
			transformer.addKeyTransformation("secret_key", properties.getSecretKeyProperty());

			if (properties.getCredentialType() == AwsCredentialType.ASSUMED_ROLE
					|| properties.getCredentialType() == AwsCredentialType.FEDERATION_TOKEN) {

				// security token transformer for STS
				transformer.addKeyTransformation("security_token", properties.getSessionTokenKeyProperty());

				return new AwsStsLeasingSecretBackendMetadata(properties, transformer);
			}
			else {
				return new AwsLeasingSecretBackendMetadata(properties, transformer);
			}
		}

		@Override
		public SecretBackendMetadata createMetadata(VaultAwsProperties backendDescriptor) {
			return forAws(backendDescriptor);
		}

		@Override
		public boolean supports(VaultSecretBackendDescriptor backendDescriptor) {
			return backendDescriptor instanceof VaultAwsProperties;
		}

		private static class AwsStsLeasingSecretBackendMetadata implements LeasingSecretBackendMetadata {

			private final VaultAwsProperties properties;

			private final PropertyNameTransformer transformer;

			AwsStsLeasingSecretBackendMetadata(VaultAwsProperties properties, PropertyNameTransformer transformer) {
				this.properties = properties;
				this.transformer = transformer;
			}

			@Override
			public String getName() {
				return String.format("%s with Role %s", this.properties.getBackend(), this.properties.getRole());
			}

			@Override
			public String getPath() {

				String defaultPath = "%s/sts/%s";
				StringJoiner joiner = new StringJoiner("&");

				// ttl for assumed_role or federation_token
				// pass through to let aws take care of min and max validations
				// per the vault role
				if (!this.properties.getTtl().isZero()) {
					joiner.add("ttl=" + this.properties.getTtl().toMillis() + "ms");
				}

				// role_arn for assumed_role for vault role that has multiple role
				// associations.
				if (this.properties.getCredentialType() == AwsCredentialType.ASSUMED_ROLE
						&& StringUtils.hasText(this.properties.getRoleArn())) {
					joiner.add("role_arn=" + this.properties.getRoleArn());
				}

				String pathToUse = joiner.length() == 0 ? defaultPath : defaultPath + "?" + joiner;

				return String.format(pathToUse, this.properties.getBackend(), this.properties.getRole());
			}

			@Override
			public PropertyTransformer getPropertyTransformer() {
				return this.transformer;
			}

			@Override
			public Map<String, String> getVariables() {

				Map<String, String> variables = new HashMap<>();

				variables.put("backend", this.properties.getBackend());
				variables.put("key", String.format("sts/%s", this.properties.getRole()));

				return variables;
			}

			@Override
			public Mode getLeaseMode() {
				return Mode.ROTATE;
			}

		}

		private static class AwsLeasingSecretBackendMetadata implements SecretBackendMetadata {

			private final VaultAwsProperties properties;

			private final PropertyNameTransformer transformer;

			AwsLeasingSecretBackendMetadata(VaultAwsProperties properties, PropertyNameTransformer transformer) {
				this.properties = properties;
				this.transformer = transformer;
			}

			@Override
			public String getName() {
				return String.format("%s with Role %s", this.properties.getBackend(), this.properties.getRole());
			}

			@Override
			public String getPath() {
				return String.format("%s/creds/%s", this.properties.getBackend(), this.properties.getRole());
			}

			@Override
			public PropertyTransformer getPropertyTransformer() {
				return this.transformer;
			}

			@Override
			public Map<String, String> getVariables() {

				Map<String, String> variables = new HashMap<>();

				variables.put("backend", this.properties.getBackend());
				variables.put("key", String.format("creds/%s", this.properties.getRole()));

				return variables;
			}

		}

	}

}
