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

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.Version;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecretBackendMetadataFactory.forDatabase;

/**
 * Integration tests for {@link VaultConfigTemplate} using the elasticsearch database
 * backend. This test requires a running Elasticearch instance, see
 * {@link #ELASTICSEARCH_HOST}. Make sure to configure {#link ES_HOME} accordingly.
 *
 * @author Mark Paluch
 */
public class ElasticsearchSecretIntegrationTests extends IntegrationTestSupport {

	private static final int ELASTICSEARCH_PORT = 9200;

	private static final String ELASTICSEARCH_HOST = "localhost";

	private static final String ES_HOME = "configure me";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultElasticsearchProperties elasticsearch = new VaultElasticsearchProperties();

	/**
	 * Initialize the elasticsearch secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT)));
		assumeTrue(prepare().getVersion().isGreaterThanOrEqualTo(Version.parse("1.3.0")));

		this.elasticsearch.setEnabled(true);
		this.elasticsearch.setRole("readonly");

		if (!prepare().hasSecretBackend(this.elasticsearch.getBackend())) {
			prepare().mountSecret(this.elasticsearch.getBackend());
		}

		VaultOperations vaultOperations = this.vaultRule.prepare().getVaultOperations();
		String database = "elasticsearch";

		Map<String, Object> config = new LinkedHashMap<>();
		config.put("plugin_name", "elasticsearch-database-plugin");
		config.put("allowed_roles", "readonly");
		config.put("username", "elastic");
		config.put("password", "elastic");
		config.put("url", String.format("http://%s:%d", ELASTICSEARCH_HOST, ELASTICSEARCH_PORT));

		config.put("ca_cert", String.format("%s/elastic-stack-ca.crt", ES_HOME));
		config.put("client_cert", String.format("%s/elastic-certificates.crt", ES_HOME));
		config.put("client_key", String.format("%s/elastic-certificates.key", ES_HOME));

		vaultOperations.write(String.format("%s/config/%s", this.elasticsearch.getBackend(), database), config);

		Map<String, Object> role = new LinkedHashMap<>();
		role.put("db_name", database);
		role.put("creation_statements",
				"{\"elasticsearch_role_definition\": {\"indices\": [{\"names\":[\"*\"], \"privileges\":[\"read\"]}]}}");
		role.put("default_ttl", "1h");

		vaultOperations.write(this.elasticsearch.getBackend() + "/roles/" + this.elasticsearch.getRole(), role);

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forDatabase(this.elasticsearch)).getData();

		assertThat(secretProperties).containsKeys("spring.elasticsearch.rest.username",
				"spring.elasticsearch.rest.password");
	}

}
