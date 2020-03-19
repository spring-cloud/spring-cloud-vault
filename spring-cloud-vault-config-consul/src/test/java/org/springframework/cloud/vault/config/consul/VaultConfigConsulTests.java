/*
 * Copyright 2016-2020 the original author or authors.
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
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.core.VaultOperations;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Integration tests using the consul secret backend. In case this test should fail
 * because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigConsulTests.TestApplication.class,
		properties = { "spring.cloud.vault.consul.enabled=true",
				"spring.cloud.vault.consul.role=readonly",
				"spring.cloud.consul.discovery.catalog-services-watch.enabled=false" })
public class VaultConfigConsulTests {

	private static final String CONSUL_HOST = "localhost";

	private static final int CONSUL_PORT = 8500;

	private static final String CONNECTION_URL = String.format("%s:%d", CONSUL_HOST,
			CONSUL_PORT);

	private static final String POLICY = "key \"\" { policy = \"read\" }";

	private static final String CONSUL_ACL_MASTER_TOKEN = "consul-master-token";

	private static final ParameterizedTypeReference<Map<String, String>> STRING_MAP = new ParameterizedTypeReference<Map<String, String>>() {
	};

	@Value("${spring.cloud.consul.discovery.acl-token:}")
	String discoveryToken;

	@Value("${spring.cloud.consul.config.acl-token:}")
	String configToken;

	@Autowired
	Environment env;

	@Autowired
	ConsulDiscoveryProperties discoveryProperties;

	@Autowired
	ConsulConfigProperties configProperties;

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

		try {
			ResponseEntity<Map<String, String>> tokenResponse = restTemplate.exchange(
					"http://{host}:{port}/v1/acl/create", HttpMethod.PUT, requestEntity,
					STRING_MAP, CONSUL_HOST, CONSUL_PORT);

			Map<String, String> consulAccess = new HashMap<>();
			consulAccess.put("address", CONNECTION_URL);
			consulAccess.put("token", tokenResponse.getBody().get("ID"));

			vaultOperations.write("consul/config/access", consulAccess);

			Map<String, Object> role = new LinkedHashMap<>();
			role.put("policy", new String(Base64.getEncoder().encode(POLICY.getBytes())));
			role.put("ttl", "15s");
			role.put("max_ttl", "15s");

			vaultOperations.write("consul/roles/readonly", role);
		}
		catch (HttpStatusCodeException e) {

			assumeFalse("Skipping because Consul is not configured as we expect it to be",
					e.getStatusCode().is4xxClientError());

			throw e;
		}
	}

	/*
	 * @Test public void shouldHaveToken() { assertThat(this.token).isNotEmpty();
	 * assertThat(this.discoveryProperties.getAclToken()).isEqualTo(this.token); }
	 */

	@Test
	public void shouldHaveRenewedToken() throws InterruptedException {
		assertThat(configToken).isNotEmpty();
		assertThat(discoveryToken).isNotEmpty();
		assertThat(this.configProperties.getAclToken()).isEqualTo(configToken);
		assertThat(this.discoveryProperties.getAclToken()).isEqualTo(discoveryToken);

		Thread.sleep(20_000L);

		// TODO: The properties weren't rebound so this test fails.
		assertThat(this.configProperties.getAclToken()).isNotEmpty()
				.isNotEqualTo(configToken);
		assertThat(this.discoveryProperties.getAclToken()).isNotEmpty()
				.isNotEqualTo(discoveryToken);
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
