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
package org.springframework.cloud.vault.config;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

/**
 * Integration test using config infrastructure with Cubbyhole authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigCubbyholeAuthenticationTests.TestApplication.class, properties = {
		"spring.cloud.vault.authentication=cubbyhole",
		"spring.cloud.vault.generic.applicationName=VaultConfigCubbyholeAuthenticationTests" })
public class VaultConfigCubbyholeAuthenticationTests {

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion()
				.isGreaterThanOrEqualTo(Version.parse("0.6.1")));

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		vaultOperations.write(
				"secret/" + VaultConfigCubbyholeAuthenticationTests.class.getSimpleName(),
				Collections.singletonMap("vault.value",
						VaultConfigCubbyholeAuthenticationTests.class.getSimpleName()));

		VaultResponse vaultResponse = vaultOperations
.doWithSession(restOperations -> {

			HttpHeaders headers = new HttpHeaders();
			headers.add("X-Vault-Wrap-TTL", "1h");

			return restOperations.postForObject("/auth/token/create", new HttpEntity<>(
					headers), VaultResponse.class);
				});

		String initialToken = vaultResponse.getWrapInfo().get("token");
		System.setProperty("spring.cloud.vault.token", initialToken);
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("spring.cloud.vault.token");
	}

	@Value("${vault.value}")
	String configValue;

	@Test
	public void contextLoads() {
		assertThat(configValue).isEqualTo(getClass().getSimpleName());
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
