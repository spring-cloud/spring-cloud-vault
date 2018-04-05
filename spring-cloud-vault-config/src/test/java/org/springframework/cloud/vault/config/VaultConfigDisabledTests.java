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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.vault.core.VaultTemplate;

import static org.assertj.core.api.Assertions.*;

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
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigDisabledTests.TestApplication.class, properties = "spring.cloud.vault.enabled=false")
public class VaultConfigDisabledTests {

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		vaultRule.prepare().getVaultOperations().write("secret/testVaultApp",
				Collections.singletonMap("vault.value", "foo"));
	}

	@Autowired
	Environment environment;

	@Autowired
	ApplicationContext applicationContext;

	@Test
	public void shouldNotContainVaultProperties() {
		assertThat(environment.containsProperty("vault.value")).isFalse();
	}

	@Test
	public void shouldNotContainVaultBeans() {

		// Beans are registered in parent (bootstrap) context.
		ApplicationContext parent = applicationContext.getParent();

		assertThat(parent.getBeanNamesForType(VaultTemplate.class)).isEmpty();
		assertThat(parent.getBeanNamesForType(VaultPropertySourceLocator.class))
				.isEmpty();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
