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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.authentication.IpAddressUserId;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test using config infrastructure with AppId authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigAppIdTests.TestApplication.class, properties = {
		"spring.cloud.vault.authentication=appid",
		"spring.cloud.vault.app-id.user-id=IP_ADDRESS",
		"spring.cloud.vault.application-name=VaultConfigAppIdTests" }) // see https://github.com/spring-cloud/spring-cloud-commons/issues/214
public class VaultConfigAppIdTests {

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		VaultProperties vaultProperties = Settings.createVaultProperties();
		vaultProperties.setAuthentication(VaultProperties.AuthenticationMethod.APPID);
		vaultProperties.getAppId().setUserId(VaultProperties.AppIdProperties.IP_ADDRESS);

		if (!vaultRule.prepare().hasAuth(vaultProperties.getAppId().getAppIdPath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getAppId().getAppIdPath());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = "{ \"name\": \"testpolicy\",\n" //
				+ "  \"path\": {\n" //
				+ "    \"*\": {  \"policy\": \"read\" }\n" //
				+ "  }\n" //
				+ "}";

		vaultOperations.write("sys/policy/testpolicy",
				Collections.singletonMap("rules", rules));

		String appId = VaultConfigAppIdTests.class.getSimpleName();

		vaultOperations.write("secret/" + VaultConfigAppIdTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		Map<String, String> appIdData = new HashMap<>();
		appIdData.put("value", "testpolicy"); // policy
		appIdData.put("display_name", "this is my test application");

		vaultOperations.write(String.format("auth/app-id/map/app-id/%s", appId),
				appIdData);

		Map<String, String> userIdData = new HashMap<>();
		userIdData.put("value", appId); // name of the app-id
		userIdData.put("cidr_block", "0.0.0.0/0");

		String userId = new IpAddressUserId().createUserId();

		vaultOperations.write(String.format("auth/app-id/map/user-id/%s", userId),
				userIdData);
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
