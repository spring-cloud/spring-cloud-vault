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

package org.springframework.cloud.vault.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using Spring Boot's ConfigData API with token authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigLoaderTests.TestApplication.class,
		properties = { "spring.cloud.vault.uri=https://localhost:8200",
				"spring.cloud.vault.application-name=config-data", "spring.config.import=vault:" })
public class VaultConfigLoaderTests {

	@Value("${vault.value}")
	String configValue;

	@Autowired
	Environment environment;

	@Autowired
	ApplicationContext applicationContext;

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		Map<String, Object> object = new HashMap<>();
		object.put("vault.value", "config-data works");
		object.put("nested", Collections.singletonMap("key", "value"));

		vaultRule.prepare().getVaultOperations().write("secret/config-data", object);
	}

	@Test
	public void contextLoads() {
		assertThat(this.configValue).isEqualTo("config-data works");
	}

	@Test
	public void shouldContainProperty() {

		assertThat(this.environment.containsProperty("vault.value")).isTrue();
		assertThat(this.environment.getProperty("vault.value")).isEqualTo("config-data works");

		assertThat(this.environment.containsProperty("nested.key")).isTrue();
		assertThat(this.environment.getProperty("nested.key")).isEqualTo("value");
	}

	@Test
	public void shouldContainVaultBeans() {

		assertThat(this.applicationContext.getBeanNamesForType(VaultTemplate.class)).isNotEmpty();
	}

	@Test
	public void shouldNotRegisterPropertySourceLocator() {

		assertThat(this.applicationContext.getBeanNamesForType(LeasingVaultPropertySourceLocator.class)).isEmpty();
	}

	@Test
	public void shouldNotContainRestTemplateArtifacts() {

		assertThat(this.applicationContext.getBeanNamesForType(RestTemplate.class)).isEmpty();
		assertThat(this.applicationContext.getBeanNamesForType(ClientHttpRequestFactory.class)).isEmpty();
	}

	@SpringBootApplication(exclude = RefreshAutoConfiguration.class)
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
