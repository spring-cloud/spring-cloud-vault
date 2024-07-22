/*
 * Copyright 2016-2021 the original author or authors.
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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultConfigAwsBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultConfigAwsBootstrapConfigurationUnitTests {

	@Test
	void shouldCreateIamTokenSecretBackendMetadataFactory() {

		VaultAwsProperties properties = new VaultAwsProperties();
		properties.setRole("readonly");

		SecretBackendMetadataFactory<VaultAwsProperties> factory = new VaultConfigAwsBootstrapConfiguration()
			.awsSecretBackendMetadataFactory();

		SecretBackendMetadata metadata = factory.createMetadata(properties);

		assertThat(metadata.getPath()).isEqualTo("aws/creds/readonly");
		assertThat(metadata.getVariables()).containsEntry("backend", "aws").containsEntry("key", "creds/readonly");
	}

	@Test
	void shouldCreateStsTokenSecretBackendMetadataFactory() {

		VaultAwsProperties properties = new VaultAwsProperties();
		properties.setCredentialType(AwsCredentialType.FEDERATION_TOKEN);
		properties.setRole("readonly");

		SecretBackendMetadataFactory<VaultAwsProperties> factory = new VaultConfigAwsBootstrapConfiguration()
			.awsSecretBackendMetadataFactory();

		SecretBackendMetadata metadata = factory.createMetadata(properties);

		assertThat(metadata.getPath()).isEqualTo("aws/sts/readonly");
		assertThat(metadata.getVariables()).containsEntry("backend", "aws").containsEntry("key", "sts/readonly");
	}

	@Test
	void shouldCreateStsTokenSecretBackendMetadataFactoryWithTtlAndRoleArn() {

		VaultAwsProperties properties = new VaultAwsProperties();
		properties.setCredentialType(AwsCredentialType.ASSUMED_ROLE);
		properties.setRoleArn("1:2:3");
		properties.setTtl(Duration.ofMinutes(1));
		properties.setRole("readonly");

		SecretBackendMetadataFactory<VaultAwsProperties> factory = new VaultConfigAwsBootstrapConfiguration()
			.awsSecretBackendMetadataFactory();

		SecretBackendMetadata metadata = factory.createMetadata(properties);

		assertThat(metadata.getPath()).isEqualTo("aws/sts/readonly?ttl=60000ms&role_arn=1:2:3");
		assertThat(metadata.getVariables()).containsEntry("backend", "aws").containsEntry("key", "sts/readonly");
	}

}
