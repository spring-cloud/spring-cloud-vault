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

package org.springframework.cloud.vault.config.databases;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.vault.core.VaultOperations;

/**
 * @author Mark Paluch
 */
class MySqlFixtures {

	static final int MYSQL_PORT = 3306;

	static final String MYSQL_HOST = "localhost";

	static final String JDBC_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT
			+ "/mysql?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

	static final String ROOT_CREDENTIALS = String.format("springvault:springvault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);

	static final String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	public static void setupMysql(VaultRule vaultRule) {

		if (!vaultRule.prepare().hasSecretBackend("database")) {
			vaultRule.prepare().mountSecret("database");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> config = new HashMap<>();
		config.put("plugin_name", "mysql-legacy-database-plugin");
		config.put("connection_url", ROOT_CREDENTIALS);
		config.put("allowed_roles", "readonly");

		vaultOperations.write("database/config/mysql", config);

		Map<String, String> body = new HashMap<>();
		body.put("db_name", "mysql");
		body.put("creation_statements", CREATE_USER_AND_GRANT_SQL);

		vaultOperations.write("database/roles/readonly", body);
	}

}
