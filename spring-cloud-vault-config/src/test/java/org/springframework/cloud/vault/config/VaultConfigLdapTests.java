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

package org.springframework.cloud.vault.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.VaultTestContextRunner;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration test using config infrastructure with LDAP authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Issam El-atif
 */
class VaultConfigLdapTests {

	private static InMemoryDirectoryServer ldapServer;

	VaultTestContextRunner contextRunner = VaultTestContextRunner.of(VaultConfigLdapTests.class)
		.withAuthentication(VaultProperties.AuthenticationMethod.LDAP)
		.withConfiguration(VaultConfigLdapTests.TestApplication.class)
		.withProperties("spring.cloud.vault.ldap.username", "testuser")
		.withProperties("spring.cloud.vault.ldap.password", "testpass")
		.withSettings(VaultTestContextRunner.TestSettings::bootstrap);

	@BeforeAll
	static void beforeClass() throws LDAPException, LDIFException {
		// LDAP server
		ldapServer = createLdapServer();
		ldapServer.startListening();

		// Vault config
		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("0.8.0")));

		VaultProperties vaultProperties = Settings.createVaultProperties();

		if (!vaultRule.prepare().hasAuth(vaultProperties.getLdap().getPath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getLdap().getPath());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = """
				{ "name": "testpolicy",
					"path": {
						"*": {  "policy": "read" }
					}
				}""";

		vaultOperations.write("sys/policy/testpolicy", Collections.singletonMap("rules", rules));

		vaultOperations.write("secret/" + VaultConfigLdapTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		Map<String, String> ldapConfig = new HashMap<>();
		ldapConfig.put("url", "ldap://localhost:" + ldapServer.getListenPort());
		ldapConfig.put("userdn", "cn=users,dc=example,dc=com");
		ldapConfig.put("groupdn", "cn=groups,dc=example,dc=com");
		ldapConfig.put("binddn", "cn=admin,dc=example,dc=com");
		ldapConfig.put("bindpass", "admin");
		ldapConfig.put("userattr", "cn");

		vaultOperations.write("auth/ldap/config", ldapConfig);

		Map<String, String> userConfig = new HashMap<>();
		userConfig.put("policies", "testpolicy");

		vaultOperations.write("auth/ldap/users/testuser", userConfig);

	}

	private static InMemoryDirectoryServer createLdapServer() throws LDAPException, LDIFException {
		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
		config.addAdditionalBindCredentials("cn=admin,dc=example,dc=com", "admin");
		config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", 0));
		config.setSchema(null); // Disable schema validation

		InMemoryDirectoryServer ldapServer = new InMemoryDirectoryServer(config);
		ldapServer.add("dn: dc=example,dc=com", "objectClass: top", "objectClass: domain", "dc: example");
		ldapServer.add("dn: cn=users,dc=example,dc=com", "objectClass: top", "objectClass: organizationalUnit",
				"ou: users");
		ldapServer.add("dn: cn=groups,dc=example,dc=com", "objectClass: top", "objectClass: organizationalUnit",
				"ou: groups");
		// Add test user
		ldapServer.add("dn: cn=testuser,cn=users,dc=example,dc=com", "objectClass: top", "objectClass: person",
				"objectClass: organizationalPerson", "objectClass: inetOrgPerson", "cn: testuser", "sn: User",
				"userPassword: testpass");

		return ldapServer;
	}

	@AfterAll
	static void afterClass() {
		if (ldapServer != null) {
			ldapServer.shutDown(true);
		}
	}

	@Test
	void contextLoads() {
		this.contextRunner.run(ctx -> {
			TestApplication application = ctx.getBean(TestApplication.class);
			assertThat(application.configValue).isEqualTo("foo");
		});
	}

	@SpringBootApplication
	public static class TestApplication {

		@Value("${vault.value}")
		String configValue;

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
