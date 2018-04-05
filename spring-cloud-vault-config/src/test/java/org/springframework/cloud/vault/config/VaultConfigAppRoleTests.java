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
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

/**
 * Integration test using config infrastructure with AppRole authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigAppRoleTests.TestApplication.class, properties = {
		"spring.cloud.vault.authentication=approle",
		"spring.cloud.vault.application-name=VaultConfigAppRoleTests" }) // see
// https://github.com/spring-cloud/spring-cloud-commons/issues/214
public class VaultConfigAppRoleTests {

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		assumeTrue(vaultRule.prepare().getVersion()
				.isGreaterThanOrEqualTo(Version.parse("0.6.1")));

		VaultProperties vaultProperties = Settings.createVaultProperties();

		if (!vaultRule.prepare().hasAuth(vaultProperties.getAppRole().getAppRolePath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getAppRole().getAppRolePath());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = "{ \"name\": \"testpolicy\",\n" //
				+ "  \"path\": {\n" //
				+ "    \"*\": {  \"policy\": \"read\" }\n" //
				+ "  }\n" //
				+ "}";

		vaultOperations.write("sys/policy/testpolicy",
				Collections.singletonMap("rules", rules));

		String appId = VaultConfigAppRoleTests.class.getSimpleName();

		vaultOperations.write("secret/" + VaultConfigAppRoleTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		Map<String, String> withSecretId = new HashMap<>();
		withSecretId.put("policies", "testpolicy"); // policy
		withSecretId.put("bound_cidr_list", "0.0.0.0/0");
		withSecretId.put("bind_secret_id", "true");

		vaultOperations.write("auth/approle/role/with-secret-id", withSecretId);

		String roleId = (String) vaultOperations
				.read("auth/approle/role/with-secret-id/role-id").getData()
				.get("role_id");
		String secretId = (String) vaultOperations
				.write(String.format("auth/approle/role/with-secret-id/secret-id",
						"with-secret-id"), null)
				.getData().get("secret_id");

		System.setProperty("spring.cloud.vault.app-role.role-id", roleId);
		System.setProperty("spring.cloud.vault.app-role.secret-id", secretId);

	}

	@Value("${vault.value}")
	String configValue;

	@Test
	public void contextLoads() {
		assertThat(configValue).isEqualTo("foo");
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
