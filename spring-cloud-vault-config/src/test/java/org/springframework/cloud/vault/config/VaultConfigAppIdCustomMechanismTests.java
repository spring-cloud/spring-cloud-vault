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

package org.springframework.cloud.vault.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.vault.AppIdUserIdMechanism;
import org.springframework.cloud.vault.config.VaultConfigAppIdCustomMechanismTests.BootstrapConfiguration;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { BootstrapConfiguration.class,
		VaultConfigAppIdCustomMechanismTests.TestApplication.class })
@IntegrationTest({ "spring.cloud.vault.authentication=appid", "use.custom.config=true",
		"spring.application.name=VaultConfigAppIdCustomMechanismTests" })
public class VaultConfigAppIdCustomMechanismTests {

	@BeforeClass
	public static void beforeClass() throws Exception {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		vaultRule.prepare().writeSecret(
				VaultConfigAppIdCustomMechanismTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		VaultProperties vaultProperties = Settings.createVaultProperties();
		vaultProperties.setAuthentication(VaultProperties.AuthenticationMethod.APPID);

		if (!vaultRule.prepare().hasAuth(vaultProperties.getAppId().getAppIdPath())) {
			vaultRule.prepare().mountAuth(vaultProperties.getAppId().getAppIdPath());
		}

		vaultRule.prepare()
				.mapAppId(VaultConfigAppIdCustomMechanismTests.class.getSimpleName());
		vaultRule.prepare().mapUserId(
				VaultConfigAppIdCustomMechanismTests.class.getSimpleName(),
				new StaticUserIdMechanism().createUserId());

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

	@Configuration
	public static class BootstrapConfiguration {

		@Bean
		@ConditionalOnProperty("use.custom.config")
		AppIdUserIdMechanism appIdUserIdMechanism() {
			return new StaticUserIdMechanism();
		}
	}

	public static class StaticUserIdMechanism implements AppIdUserIdMechanism {

		@Override
		public String createUserId() {
			return "static-string";
		}
	}
}
