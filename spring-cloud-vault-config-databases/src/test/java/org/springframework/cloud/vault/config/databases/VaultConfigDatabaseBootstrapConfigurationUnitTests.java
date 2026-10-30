/*
 * Copyright 2018-present the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultConfigDatabaseBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultConfigDatabaseBootstrapConfigurationUnitTests {

	@Test
	public void shouldConsiderCredentialPath() {

		VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory factory = new VaultConfigDatabaseBootstrapConfiguration()
			.databaseSecretBackendMetadataFactory();

		VaultDatabaseProperties properties = new VaultDatabaseProperties();
		properties.setStaticRole(true);
		properties.setRole("my-role");

		SecretBackendMetadata metadata = factory.createMetadata(properties);

		assertThat(metadata.getPath()).isEqualTo("database/static-creds/my-role");
	}

	@Test
	public void shouldContributePropertiesForSameRoleWithDifferentTargetProperties() {

		VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory factory = new VaultConfigDatabaseBootstrapConfiguration()
			.databaseSecretBackendMetadataFactory();

		VaultDatabaseProperties primary = databaseProperties("db1-dbuser", "spring.datasource.username",
				"spring.datasource.password");
		VaultDatabaseProperties secondary = databaseProperties("db1-dbuser", "spring.secondary-datasource.username",
				"spring.secondary-datasource.password");

		SecretBackendMetadata primaryMetadata = factory.createMetadata(primary);
		SecretBackendMetadata secondaryMetadata = factory.createMetadata(secondary);
		Map<String, Object> secret = Map.of("username", "vault-user", "password", "vault-password");

		CompositePropertySource propertySource = new CompositePropertySource("vault");
		propertySource.addPropertySource(new MapPropertySource(primaryMetadata.getName(),
				primaryMetadata.getPropertyTransformer().transformProperties(secret)));
		propertySource.addPropertySource(new MapPropertySource(secondaryMetadata.getName(),
				secondaryMetadata.getPropertyTransformer().transformProperties(secret)));

		assertThat(propertySource.getProperty("spring.datasource.username")).isEqualTo("vault-user");
		assertThat(propertySource.getProperty("spring.datasource.password")).isEqualTo("vault-password");
		assertThat(propertySource.getProperty("spring.secondary-datasource.username")).isEqualTo("vault-user");
		assertThat(propertySource.getProperty("spring.secondary-datasource.password")).isEqualTo("vault-password");
	}

	private static VaultDatabaseProperties databaseProperties(String role, String usernameProperty,
			String passwordProperty) {

		VaultDatabaseProperties properties = new VaultDatabaseProperties();
		properties.setRole(role);
		properties.setUsernameProperty(usernameProperty);
		properties.setPasswordProperty(passwordProperty);
		return properties;
	}

}
