/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.cloud.vault.config.consul;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assume.assumeFalse;

/**
 * Utility to setup Consul.
 *
 * @author Mark Paluch
 */
final class SetupConsul {

	static final String CONSUL_HOST = "localhost";

	static final int CONSUL_PORT = 8500;

	private static final String CONNECTION_URL = String.format("%s:%d", CONSUL_HOST, CONSUL_PORT);

	private static final ParameterizedTypeReference<Map<String, String>> STRING_MAP = new ParameterizedTypeReference<Map<String, String>>() {
	};

	private static final String CONSUL_ACL_MASTER_TOKEN = "consul-master-token";

	private SetupConsul() {
	}

	static void setupConsul(VaultOperations vaultOperations, String consulBackend) {

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Consul-Token", CONSUL_ACL_MASTER_TOKEN);
		HttpEntity<String> requestEntity = new HttpEntity<>("{\"Name\": \"sample\", \"Type\": \"management\"}",
				headers);

		try {
			ResponseEntity<Map<String, String>> tokenResponse = restTemplate.exchange(
					"http://{host}:{port}/v1/acl/create", HttpMethod.PUT, requestEntity, STRING_MAP, CONSUL_HOST,
					CONSUL_PORT);

			Map<String, String> consulAccess = new HashMap<>();
			consulAccess.put("address", CONNECTION_URL);
			consulAccess.put("token", tokenResponse.getBody().get("ID"));

			vaultOperations.write(String.format("%s/config/access", consulBackend), consulAccess);
		}
		catch (HttpStatusCodeException e) {

			assumeFalse("Skipping because Consul is not configured as we expect it to be",
					e.getStatusCode().is4xxClientError());

			throw e;
		}
	}

	static boolean isConsulAvailable() {
		return CanConnect.to(new InetSocketAddress(CONSUL_HOST, CONSUL_PORT));
	}

}
