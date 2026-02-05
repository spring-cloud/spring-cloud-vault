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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.config.ldap.VaultConfigLdapBootstrapConfiguration.LdapSecretBackendMetadataFactory;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link VaultConfigTemplate} using the LDAP secret engine with an
 * in-memory UnboundID LDAP server.
 *
 * @author Drew Mullen
 */
public class LdapSecretIntegrationTests extends IntegrationTestSupport {

	@RegisterExtension
	public static final LdapServerExtension ldapServer = new LdapServerExtension();

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultLdapProperties ldapProperties = new VaultLdapProperties();

	/**
	 * Initialize the LDAP secret engine configuration with the in-memory LDAP server.
	 */
	@BeforeEach
	public void setUp() {
		this.ldapProperties.setEnabled(true);
		this.ldapProperties.setBackend("ldap");

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		if (!prepare().hasSecretBackend(this.ldapProperties.getBackend())) {
			prepare().mountSecret(this.ldapProperties.getBackend());
		}

		Map<String, Object> ldapConfig = new HashMap<>();
		ldapConfig.put("binddn", "uid=vault-admin,ou=people," + ldapServer.getBaseDn());
		ldapConfig.put("bindpass", "vault-admin-password");
		ldapConfig.put("url", ldapServer.getLdapUrl());
		ldapConfig.put("userdn", "ou=people," + ldapServer.getBaseDn());

		vaultOperations.write(String.format("%s/config", this.ldapProperties.getBackend()), ldapConfig);

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	/**
	 * Test for dynamic role credential retrieval.
	 */
	@Test
	public void shouldCreateDynamicCredentialsCorrectly() {
		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, Object> dynamicRoleConfig = new HashMap<>();
		dynamicRoleConfig.put("creation_ldif",
				"dn: cn={{.Username}},ou=people," + ldapServer.getBaseDn() + "\n" + "objectClass: person\n"
						+ "objectClass: top\n" + "cn: {{.Username}}\n" + "sn: {{.Username}}\n"
						+ "userPassword: {{.Password}}");
		dynamicRoleConfig.put("deletion_ldif",
				"dn: cn={{.Username}},ou=people," + ldapServer.getBaseDn() + "\nchangetype: delete");
		dynamicRoleConfig.put("default_ttl", "1h");
		dynamicRoleConfig.put("max_ttl", "24h");

		vaultOperations.write(String.format("%s/role/dynamic-role", this.ldapProperties.getBackend()),
				dynamicRoleConfig);

		this.ldapProperties.setRole("dynamic-role");
		this.ldapProperties.setStaticRole(false);

		Map<String, Object> secretProperties = this.configOperations
			.read(LdapSecretBackendMetadataFactory.forLdap(this.ldapProperties))
			.getData();

		assertThat(secretProperties).containsKeys("spring.ldap.username", "spring.ldap.password");
	}

	/**
	 * Test for static role credential retrieval.
	 */
	@Test
	public void shouldCreateStaticCredentialsCorrectly() {
		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();

		Map<String, Object> staticRoleConfig = new HashMap<>();
		staticRoleConfig.put("dn", "uid=static-user,ou=people," + ldapServer.getBaseDn());
		staticRoleConfig.put("username", "static-user");
		staticRoleConfig.put("rotation_period", "24h");

		vaultOperations.write(String.format("%s/static-role/static-role", this.ldapProperties.getBackend()),
				staticRoleConfig);

		this.ldapProperties.setRole("static-role");
		this.ldapProperties.setStaticRole(true);

		Map<String, Object> secretProperties = this.configOperations
			.read(LdapSecretBackendMetadataFactory.forLdap(this.ldapProperties))
			.getData();

		assertThat(secretProperties).containsKeys("spring.ldap.username", "spring.ldap.password");
	}

}
