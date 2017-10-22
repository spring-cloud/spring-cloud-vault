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
package org.springframework.cloud.vault.config.databases;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the cassandra secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigDatabaseTests.TestApplication.class, properties = {
        "spring.cloud.vault.database.enabled=true",
        "spring.cloud.vault.database.role=readonly",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres?ssl=false",
})
public class VaultConfigDatabaseTests {

    private final static String POSTGRES_HOST = "localhost";
    private final static int POSTGRES_PORT = 5432;

    private final static String CONNECTION_URL = String.format(
            "postgresql://springvault:springvault@%s:%d/postgres?sslmode=disable", POSTGRES_HOST,
            POSTGRES_PORT);

    private final static String CREATE_USER_AND_GRANT_SQL = "CREATE ROLE \"{{name}}\" WITH "
            + "LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';\n"
            + "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";";
    private static final String ROLE_NAME = "readonly";
    private static final String BACKEND = "database";

    /**
     * Initialize the database secret backend.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

        assumeTrue(CanConnect.to(new InetSocketAddress(POSTGRES_HOST, POSTGRES_PORT)));

        VaultRule vaultRule = new VaultRule();
        vaultRule.before();

        if (!vaultRule.prepare().hasSecretBackend(BACKEND)) {
            vaultRule.prepare().mountSecret(BACKEND);
        }

        VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

        Map<String, String> connection = new HashMap<>();
        connection.put("plugin_name", "postgresql-database-plugin");
        connection.put("allowed_roles", ROLE_NAME);
        connection.put("connection_url", CONNECTION_URL);
        vaultOperations.write(String.format("%s/config/postgresql", BACKEND), connection);

        Map<String, String> role = new HashMap<>();

        role.put("creation_statements", CREATE_USER_AND_GRANT_SQL);
        role.put("db_name", "postgresql");

        vaultOperations.write(String.format("%s/roles/%s", BACKEND, "readonly"), role);

    }

    @Value("${spring.datasource.username}")
    String username;

    @Value("${spring.datasource.password}")
    String password;

    @Autowired
    DataSource dataSource;

    @Test
    public void shouldConnectUsingDataSource() throws SQLException {

        Connection connection = dataSource.getConnection();

        assertThat(connection.getSchema()).isEqualTo("public");
        connection.close();
    }

    @Test
    public void shouldConnectUsingJdbcUrlConnection() throws SQLException {

        String url = String.format("jdbc:postgresql://%s:%d/postgres?ssl=false",
                POSTGRES_HOST, POSTGRES_PORT);
        DriverManager.getConnection(url, username, password).close();
    }

    @SpringBootApplication
    public static class TestApplication {

        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}
