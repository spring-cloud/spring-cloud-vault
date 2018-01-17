/*
 * Copyright 2017-2018 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.config.GenericSecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.consul.VaultConfigConsulBootstrapConfiguration.ConsulSecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.consul.VaultConfigConsulBootstrapConfigurationTests.CustomBootstrapConfiguration;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VaultConfigConsulBootstrapConfigurationTests}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CustomBootstrapConfiguration.class, properties = {
		"VaultConfigConsulBootstrapConfigurationTests.custom.config=true",
		"spring.cloud.vault.consul.role=foo", "spring.cloud.vault.consul.enabled=true",
		"spring.cloud.consul.config.enabled=false", })
public class VaultConfigConsulBootstrapConfigurationTests extends IntegrationTestSupport {

	@Autowired
	ConsulSecretBackendMetadataFactory factory;

	@Autowired
	VaultConsulProperties properties;

	@Test
	public void shouldApplyCustomConfiguration() {

		SecretBackendMetadata metadata = factory.createMetadata(properties);

		assertThat(metadata).isInstanceOf(GenericSecretBackendMetadata.class);
		assertThat(metadata.getPath()).isEqualTo(properties.getRole());
	}

	@Configuration
	public static class CustomBootstrapConfiguration {

		@Bean
		@ConditionalOnProperty("VaultConfigConsulBootstrapConfigurationTests.custom.config")
		ConsulSecretBackendMetadataFactory customFactory() {

			return new ConsulSecretBackendMetadataFactory() {
				@Override
				public SecretBackendMetadata createMetadata(
						VaultConsulProperties backendDescriptor) {
					return GenericSecretBackendMetadata.create(backendDescriptor
							.getRole());
				}
			};
		}
	}
}
