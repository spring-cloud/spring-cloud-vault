/*
 * Copyright 2016-present the original author or authors.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.lease.ManagedSecret;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test using config infrastructure with token authentication.
 *
 * <p>
 * In case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Mark Paluch
 */
@SpringBootTest(classes = VaultConfigTests.TestApplication.class,
		properties = { "spring.cloud.vault.application-name=testVaultApp", "spring.cloud.bootstrap.enabled=true" })
public class VaultConfigTests {

	@Value("${vault.value}")
	String configValue;

	@Autowired
	Environment environment;

	@Autowired
	ApplicationContext applicationContext;

	@BeforeAll
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		Map<String, Object> object = new HashMap<>();
		object.put("vault.value", "foo");
		object.put("nested", Collections.singletonMap("key", "value"));

		vaultRule.prepare().getVaultOperations().write("secret/testVaultApp", object);
		vaultRule.prepare().getVaultOperations().write("secret/managed", object);
	}

	@Test
	public void contextLoads() {
		assertThat(this.configValue).isEqualTo("foo");
	}

	@Test
	public void shouldContainProperty() {

		assertThat(this.environment.containsProperty("vault.value")).isTrue();
		assertThat(this.environment.getProperty("vault.value")).isEqualTo("foo");

		assertThat(this.environment.containsProperty("nested.key")).isTrue();
		assertThat(this.environment.getProperty("nested.key")).isEqualTo("value");
	}

	@Test
	public void shouldContainVaultBeans() {

		// Beans are registered in parent (bootstrap) context.
		ApplicationContext parent = this.applicationContext.getParent();

		assertThat(parent.getBeanNamesForType(VaultTemplate.class)).isNotEmpty();
		assertThat(parent.getBeanNamesForType(LeasingVaultPropertySourceLocator.class)).isNotEmpty();
	}

	@Test
	public void shouldRegisterManagedSecret() throws Exception {

		TestApplication app = this.applicationContext.getBean(TestApplication.class);
		String secret = app.secret.get(5, TimeUnit.SECONDS);

		assertThat(secret).isEqualTo("foo");
	}

	@Test
	public void shouldNotContainRestTemplateArtifacts() {

		// Beans are registered in parent (bootstrap) context.
		ApplicationContext parent = this.applicationContext.getParent();

		assertThat(parent.getBeanNamesForType(RestTemplate.class)).isEmpty();
		assertThat(parent.getBeanNamesForType(ClientHttpRequestFactory.class)).isEmpty();
	}

	@SpringBootApplication
	public static class TestApplication {

		final CompletableFuture<String> secret = new CompletableFuture<>();

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

		@Bean
		ManagedSecret mySecret() {
			return ManagedSecret.rotating("secret/managed", it -> {
				TestApplication.this.secret.complete(it.getString("vault.value"));
			}, this.secret::completeExceptionally);
		}

	}

}
