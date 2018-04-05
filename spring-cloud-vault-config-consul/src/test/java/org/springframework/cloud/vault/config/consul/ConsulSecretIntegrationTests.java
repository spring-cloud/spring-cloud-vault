/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.vault.config.consul;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.consul.VaultConfigConsulBootstrapConfiguration.ConsulSecretBackendMetadataFactory.*;

/**
 * Integration tests for {@link VaultConfigTemplate} using the consul secret backend. This
 * test requires a running Consul instance, see {@link #CONNECTION_URL}.
 *
 * @author Mark Paluch
 */
public class ConsulSecretIntegrationTests extends IntegrationTestSupport {

	private final static String CONSUL_HOST = "localhost";
	private final static int CONSUL_PORT = 8500;

	private final static String CONNECTION_URL = String.format("%s:%d", CONSUL_HOST,
			CONSUL_PORT);

	private final static String POLICY = "key \"\" { policy = \"read\" }";
	private final static String CONSUL_ACL_MASTER_TOKEN = "consul-master-token";

	private final static ParameterizedTypeReference<Map<String, String>> STRING_MAP = new ParameterizedTypeReference<Map<String, String>>() {
	};

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	private VaultConsulProperties consul = new VaultConsulProperties();
	private RestTemplate restTemplate = new RestTemplate();

	/**
	 * Initialize the consul secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(CanConnect.to(new InetSocketAddress(CONSUL_HOST, CONSUL_PORT)));

		consul.setEnabled(true);
		consul.setRole("readonly");

		if (!prepare().hasSecretBackend(consul.getBackend())) {
			prepare().mountSecret(consul.getBackend());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Consul-Token", CONSUL_ACL_MASTER_TOKEN);
		HttpEntity<String> requestEntity = new HttpEntity<>(
				"{\"Name\": \"sample\", \"Type\": \"management\"}", headers);
		ResponseEntity<Map<String, String>> tokenResponse = restTemplate.exchange(
				"http://{host}:{port}/v1/acl/create", HttpMethod.PUT, requestEntity,
				STRING_MAP, CONSUL_HOST, CONSUL_PORT);

		Map<String, String> consulAccess = new HashMap<>();
		consulAccess.put("address", CONNECTION_URL);
		consulAccess.put("token", tokenResponse.getBody().get("ID"));

		vaultOperations.write(String.format("%s/config/access", consul.getBackend()),
				consulAccess);

		vaultOperations.write(
				String.format("%s/roles/%s", consul.getBackend(), consul.getRole()),
				Collections.singletonMap("policy",
						Base64Utils.encodeToString(POLICY.getBytes())));

		configOperations = new VaultConfigTemplate(vaultOperations, vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = configOperations.read(forConsul(consul))
				.getData();

		assertThat(secretProperties).containsKeys("spring.cloud.consul.token");
	}
}
