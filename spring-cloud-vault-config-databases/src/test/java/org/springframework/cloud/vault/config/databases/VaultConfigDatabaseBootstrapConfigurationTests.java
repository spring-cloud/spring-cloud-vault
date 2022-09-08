/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.cloud.vault.config.databases;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.config.KeyValueSecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfigurationTests.CustomBootstrapConfiguration;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VaultConfigDatabaseBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CustomBootstrapConfiguration.class,
		properties = { "VaultConfigDatabaseBootstrapConfigurationTests.custom.config=true",
				"spring.cloud.vault.mysql.role=foo", "spring.cloud.vault.mysql.enabled=true",
				"spring.cloud.bootstrap.enabled=true" })
public class VaultConfigDatabaseBootstrapConfigurationTests extends IntegrationTestSupport {

	@Autowired
	@Qualifier("customFactory")
	DatabaseSecretBackendMetadataFactory factory;

	@SuppressWarnings("deprecation")
	@Autowired
	VaultMySqlProperties properties;

	@Test
	public void shouldApplyCustomConfiguration() {

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		assertThat(metadata).isInstanceOf(KeyValueSecretBackendMetadata.class);
		assertThat(metadata.getPath()).isEqualTo(this.properties.getRole());
	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomBootstrapConfiguration {

		@Bean
		@ConditionalOnProperty("VaultConfigDatabaseBootstrapConfigurationTests.custom.config")
		DatabaseSecretBackendMetadataFactory customFactory() {

			return new DatabaseSecretBackendMetadataFactory() {
				@Override
				public SecretBackendMetadata createMetadata(DatabaseSecretProperties backendDescriptor) {
					return KeyValueSecretBackendMetadata.create(backendDescriptor.getRole());
				}
			};
		}

	}

}
