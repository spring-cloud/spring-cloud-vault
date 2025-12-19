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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.config.ldap.VaultConfigLdapBootstrapConfiguration.LdapSecretBackendMetadataFactory;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link VaultConfigTemplate} using the LDAP secret engine. These
 * tests require a running LDAP instance and properly configured Vault LDAP secret engine.
 * <p>
 * Note: This test will be skipped if the LDAP secret engine is not available or not
 * configured in Vault.
 *
 * @author Drew Mullen
 */
@Disabled
public class LdapSecretIntegrationTests extends IntegrationTestSupport {

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultLdapProperties ldapProperties = new VaultLdapProperties();

	/**
	 * Initialize the LDAP secret engine configuration. Tests will be skipped if the LDAP
	 * secret engine is not available.
	 */
	@BeforeEach
	public void setUp() {
		// Skip tests if LDAP secret engine is not mounted/available
		assumeTrue(isLdapSecretEngineAvailable(), "LDAP secret engine is not available or configured");

		this.ldapProperties.setEnabled(true);
		this.ldapProperties.setBackend("ldap");

		this.configOperations = new VaultConfigTemplate(this.vaultRule.prepare().getVaultOperations(),
				this.vaultProperties);
	}

	/**
	 * Test for dynamic role credential retrieval. This test requires a configured dynamic
	 * role in Vault's LDAP secret engine.
	 */
	@Test
	public void shouldCreateDynamicCredentialsCorrectly() {
		// This test requires a pre-configured dynamic role named "dynamic-role" in Vault
		assumeTrue(isDynamicRoleAvailable("dynamic-role"), "Dynamic role 'dynamic-role' is not configured");

		this.ldapProperties.setRole("dynamic-role");
		this.ldapProperties.setStaticRole(false);

		Map<String, Object> secretProperties = this.configOperations
			.read(LdapSecretBackendMetadataFactory.forLdap(this.ldapProperties))
			.getData();

		assertThat(secretProperties).containsKeys("spring.ldap.username", "spring.ldap.password");
	}

	/**
	 * Test for static role credential retrieval. This test requires a configured static
	 * role in Vault's LDAP secret engine.
	 */
	@Test
	public void shouldCreateStaticCredentialsCorrectly() {
		// This test requires a pre-configured static role named "static-role" in Vault
		assumeTrue(isStaticRoleAvailable("static-role"), "Static role 'static-role' is not configured");

		this.ldapProperties.setRole("static-role");
		this.ldapProperties.setStaticRole(true);

		Map<String, Object> secretProperties = this.configOperations
			.read(LdapSecretBackendMetadataFactory.forLdap(this.ldapProperties))
			.getData();

		assertThat(secretProperties).containsKeys("spring.ldap.username", "spring.ldap.password");
	}

	/**
	 * Check if the LDAP secret engine is available in Vault. This can be determined by
	 * attempting to read the LDAP config endpoint.
	 */
	private boolean isLdapSecretEngineAvailable() {
		try {
			this.vaultRule.prepare().getVaultOperations().read("ldap/config");
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check if a dynamic role exists in Vault.
	 */
	private boolean isDynamicRoleAvailable(String roleName) {
		try {
			this.vaultRule.prepare().getVaultOperations().read("ldap/role/" + roleName);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check if a static role exists in Vault.
	 */
	private boolean isStaticRoleAvailable(String roleName) {
		try {
			this.vaultRule.prepare().getVaultOperations().read("ldap/static-role/" + roleName);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

}
