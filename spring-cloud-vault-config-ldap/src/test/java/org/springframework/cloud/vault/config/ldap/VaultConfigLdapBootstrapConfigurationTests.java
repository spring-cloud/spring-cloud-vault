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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.ldap.VaultConfigLdapBootstrapConfiguration.LdapSecretBackendMetadataFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VaultConfigLdapBootstrapConfiguration}.
 *
 * @author Drew Mullen
 */
public class VaultConfigLdapBootstrapConfigurationTests {

	private LdapSecretBackendMetadataFactory factory = new LdapSecretBackendMetadataFactory();

	private VaultLdapProperties properties = new VaultLdapProperties();

	@Test
	public void shouldCreateDynamicRoleMetadata() {

		this.properties.setEnabled(true);
		this.properties.setRole("my-role");
		this.properties.setStaticRole(false);

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		assertThat(metadata.getName()).isEqualTo("ldap with Role my-role");
		assertThat(metadata.getPath()).isEqualTo("ldap/creds/my-role");
		assertThat(metadata.getVariables()).containsEntry("backend", "ldap").containsEntry("key", "creds/my-role");
	}

	@Test
	public void shouldCreateStaticRoleMetadata() {

		this.properties.setEnabled(true);
		this.properties.setRole("my-static-role");
		this.properties.setStaticRole(true);

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		assertThat(metadata.getName()).isEqualTo("ldap with Role my-static-role");
		assertThat(metadata.getPath()).isEqualTo("ldap/static-cred/my-static-role");
		assertThat(metadata.getVariables()).containsEntry("backend", "ldap")
			.containsEntry("key", "static-cred/my-static-role");
	}

	@Test
	public void shouldCreateMetadataWithCustomBackend() {

		this.properties.setEnabled(true);
		this.properties.setRole("my-role");
		this.properties.setBackend("custom-ldap");

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		assertThat(metadata.getName()).isEqualTo("custom-ldap with Role my-role");
		assertThat(metadata.getPath()).isEqualTo("custom-ldap/creds/my-role");
		assertThat(metadata.getVariables()).containsEntry("backend", "custom-ldap")
			.containsEntry("key", "creds/my-role");
	}

	@Test
	public void shouldTransformProperties() {

		this.properties.setEnabled(true);
		this.properties.setRole("my-role");

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		Map<String, Object> input = new HashMap<>();
		input.put("username", "test-user");
		input.put("password", "test-pass");

		Map<String, Object> transformed = metadata.getPropertyTransformer().transformProperties(input);

		assertThat(transformed).containsEntry("spring.ldap.username", "test-user")
			.containsEntry("spring.ldap.password", "test-pass");
	}

	@Test
	public void shouldTransformPropertiesWithCustomPropertyNames() {

		this.properties.setEnabled(true);
		this.properties.setRole("my-role");
		this.properties.setUsernameProperty("custom.username");
		this.properties.setPasswordProperty("custom.password");

		SecretBackendMetadata metadata = this.factory.createMetadata(this.properties);

		Map<String, Object> input = new HashMap<>();
		input.put("username", "test-user");
		input.put("password", "test-pass");

		Map<String, Object> transformed = metadata.getPropertyTransformer().transformProperties(input);

		assertThat(transformed).containsEntry("custom.username", "test-user")
			.containsEntry("custom.password", "test-pass");
	}

	@Test
	public void shouldSupportVaultLdapProperties() {

		VaultLdapProperties properties = new VaultLdapProperties();

		assertThat(this.factory.supports(properties)).isTrue();
	}

}
