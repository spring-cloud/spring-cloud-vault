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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Java6Assertions.*;
import static org.junit.Assume.*;

/**
 * Integration tests using the consul secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigConsulTests.TestApplication.class, properties = {
		"spring.cloud.vault.consul.enabled=true",
		"spring.cloud.vault.consul.role=readonly" })
public class VaultConfigConsulTests {

	private final static String CONSUL_HOST = "localhost";
	private final static int CONSUL_PORT = 8500;

	private final static String CONNECTION_URL = String.format("%s:%d", CONSUL_HOST,
			CONSUL_PORT);

	private final static String POLICY = "key \"\" { policy = \"read\" }";
	private final static String CONSUL_ACL_MASTER_TOKEN = "consul-master-token";

	private final static ParameterizedTypeReference<Map<String, String>> STRING_MAP = new ParameterizedTypeReference<Map<String, String>>() {
	};

	/**
	 * Initialize the consul secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		assumeTrue(CanConnect.to(new InetSocketAddress(CONSUL_HOST, CONSUL_PORT)));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecretBackend("consul")) {
			vaultRule.prepare().mountSecret("consul");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();
		RestTemplate restTemplate = new RestTemplate();

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

		vaultOperations.write("consul/config/access", consulAccess);

		vaultOperations.write("consul/roles/readonly", Collections.singletonMap("policy",
				new String(Base64.getEncoder().encode(POLICY.getBytes()))));
	}

	@Value("${spring.cloud.consul.token}")
	String token;

	@Test
	public void shouldHaveToken() {
		assertThat(token).isNotEmpty();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
