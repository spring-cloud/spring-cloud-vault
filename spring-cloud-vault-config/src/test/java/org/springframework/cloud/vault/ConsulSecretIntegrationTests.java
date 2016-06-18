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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for {@link VaultClient} using the consul secret backend. This test
 * requires a running Consul instance, see {@link #CONNECTION_URL}.
 *
 * @author Mark Paluch
 */
public class ConsulSecretIntegrationTests extends AbstractIntegrationTests {

	private final static String CONSUL_HOST = "localhost";
	private final static int CONSUL_PORT = 8500;

	private final static String CONNECTION_URL = String.format("%s:%d", CONSUL_HOST,
			CONSUL_PORT);

	private final static String POLICY = "key \"\" { policy = \"read\" }";
	private final static String CONSUL_ACL_MASTER_TOKEN = "consul-master-token";

	private final static ParameterizedTypeReference<Map<String, String>> STRING_MAP = new ParameterizedTypeReference<Map<String, String>>() {
	};

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultClient vaultClient = new VaultClient(vaultProperties);
	private VaultProperties.Consul consul = vaultProperties.getConsul();

	private TestRestTemplate restTemplate = new TestRestTemplate();

	/**
	 * Initialize the postgresql secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(CONSUL_HOST, CONSUL_PORT)));

		consul.setEnabled(true);
		consul.setRole("readonly");

		if (!prepare().hasSecret(consul.getBackend())) {
			prepare().mountSecret(consul.getBackend());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Consul-Token", CONSUL_ACL_MASTER_TOKEN);
		HttpEntity<String> requestEntity = new HttpEntity<>(
				"{\"Name\": \"sample\", \"Type\": \"management\"}", headers);
		ResponseEntity<Map<String, String>> tokenResponse = restTemplate.exchange(
				"http://{address}/v1/acl/create", HttpMethod.PUT, requestEntity,
				STRING_MAP, CONNECTION_URL);

		Map<String, String> consulAccess = new HashMap<>();
		consulAccess.put("address", CONNECTION_URL);
		consulAccess.put("token", tokenResponse.getBody().get("ID"));

		prepare().write(String.format("%s/config/access", consul.getBackend()),
				consulAccess);

		prepare().write(
				String.format("%s/roles/%s", consul.getBackend(), consul.getRole()),
				Collections.singletonMap("policy",
						Base64.encodeBase64String(POLICY.getBytes())));

		vaultClient.setRest(TestRestTemplateFactory.create(vaultProperties));
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = vaultClient.read(consul(consul),
				Settings.token());

		assertThat(secretProperties).containsKeys("spring.cloud.consul.token");
	}
}
