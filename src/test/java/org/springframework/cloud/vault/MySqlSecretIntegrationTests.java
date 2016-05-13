/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.vault;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.SecureBackendAccessors.*;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for {@link VaultClient} using the mysql secret backend. This test
 * requires a running MySQL instance, see {@link #ROOT_CREDENTIALS}.
 *
 * @author Mark Paluch
 */
public class MySqlSecretIntegrationTests extends AbstractIntegrationTests {

	private final static int MYSQL_PORT = 3306;
	private final static String MYSQL_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String
			.format("spring:vault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);
	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultClient vaultClient = new VaultClient(vaultProperties);
	private VaultProperties.MySql mySql = vaultProperties.getMysql();

	/**
	 * Initialize the mysql secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));

		mySql.setEnabled(true);
		mySql.setRole("readonly");

		if (!prepare().hasSecret(mySql.getBackend())) {
			prepare().mountSecret(mySql.getBackend());
		}

		prepare().write(String.format("%s/config/connection", mySql.getBackend()),
				Collections.singletonMap("connection_url", ROOT_CREDENTIALS));

		prepare().write(String.format("%s/roles/%s", mySql.getBackend(), mySql.getRole()),
				Collections.singletonMap("sql", CREATE_USER_AND_GRANT_SQL));

		vaultClient.setRest(new RestTemplate());
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = vaultClient.read(database(mySql),
				Settings.token());

		assertThat(secretProperties).containsKeys("spring.datasource.username",
				"spring.datasource.password");
	}

}
