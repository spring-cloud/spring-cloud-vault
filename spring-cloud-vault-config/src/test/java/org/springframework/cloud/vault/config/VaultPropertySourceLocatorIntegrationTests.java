package org.springframework.cloud.vault.config;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test incorporating loading secrets using {@code spring.cloud.vault.applicationName}
 * and active profiles
 *
 * @author Ryan Hoegg
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultPropertySourceLocatorIntegrationTests.TestApplication.class)
public class VaultPropertySourceLocatorIntegrationTests extends IntegrationTestSupport {
		@BeforeClass
		public static void beforeClass() throws Exception {
				System.setProperty("spring.application.name", "wintermute");
				System.setProperty("spring.cloud.vault.applicationName", "neuromancer");
				System.setProperty("spring.profiles.active", "integrationtest");
				VaultRule vaultRule = new VaultRule();
				vaultRule.before();

				vaultRule.prepare().getVaultOperations().write("secret/wintermute",
						Collections.singletonMap("vault.value", "spring.application.name value"));
				vaultRule.prepare().getVaultOperations().write("secret/wintermute/integrationtest",
						Collections.singletonMap("vault.value", "spring.application.name:integrationtest value"));
				vaultRule.prepare().getVaultOperations().write("secret/neuromancer",
						Collections.singletonMap("vault.value", "spring.cloud.vault.applicationName value"));
				vaultRule.prepare().getVaultOperations().write("secret/neuromancer/integrationtest",
						Collections.singletonMap("vault.value", "spring.cloud.vault.applicationName:integrationtest value"));
		}

		@Before
		public void before() throws Exception {
		}

		@Value("${vault.value}")
		String configValue;

		@Test
		public void getsSecretFromVaultUsingVaultApplicationName() {
				assertThat(configValue).isEqualTo("spring.cloud.vault.applicationName:integrationtest value");
		}

		@SpringBootApplication
		public static class TestApplication {

				public static void main(String[] args) {
						SpringApplication.run(VaultConfigWithContextTests.TestApplication.class, args);
				}
		}

}
