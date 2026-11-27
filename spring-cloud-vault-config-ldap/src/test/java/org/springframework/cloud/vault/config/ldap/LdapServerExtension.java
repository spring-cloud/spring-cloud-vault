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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for managing an in-memory UnboundID LDAP server for testing.
 *
 * @author Drew Mullen
 */
public class LdapServerExtension implements BeforeAllCallback, AfterAllCallback {

	private InMemoryDirectoryServer ldapServer;

	private int ldapPort = 10389;

	private String baseDn = "dc=springframework,dc=org";

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(this.baseDn);

		config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", this.ldapPort));

		this.ldapServer = new InMemoryDirectoryServer(config);
		this.ldapServer.add("dn: " + this.baseDn, "objectClass: top", "objectClass: domain", "dc: springframework");
		this.ldapServer.add("dn: ou=people," + this.baseDn, "objectClass: organizationalUnit", "ou: people");
		this.ldapServer.add("dn: uid=static-user,ou=people," + this.baseDn, "objectClass: inetOrgPerson",
				"objectClass: organizationalPerson", "objectClass: person", "objectClass: top", "cn: Static User",
				"sn: User", "uid: static-user", "userPassword: initial-password");

		this.ldapServer.add("dn: uid=vault-admin,ou=people," + this.baseDn, "objectClass: inetOrgPerson",
				"objectClass: organizationalPerson", "objectClass: person", "objectClass: top", "cn: Vault Admin",
				"sn: Admin", "uid: vault-admin", "userPassword: vault-admin-password");

		this.ldapServer.startListening();
	}

	@Override
	public void afterAll(ExtensionContext context) {
		if (this.ldapServer != null) {
			this.ldapServer.shutDown(true);
		}
	}

	public int getLdapPort() {
		return this.ldapPort;
	}

	public String getBaseDn() {
		return this.baseDn;
	}

	public String getLdapUrl() {
		return "ldap://localhost:" + this.ldapPort;
	}

	public InMemoryDirectoryServer getLdapServer() {
		return this.ldapServer;
	}

}
