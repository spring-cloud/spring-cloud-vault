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
import org.springframework.test.context.ActiveProfiles;
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
@SpringBootTest(classes = VaultPropertySourceLocatorIntegrationTests.TestApplication.class, properties = {
	"spring.application.name=wintermute",
	"spring.cloud.vault.application-name=neuromancer",
	"spring.cloud.vault.generic.application-name=neuromancer,icebreaker"
})
@ActiveProfiles({"integrationtest"})
public class VaultPropertySourceLocatorIntegrationTests extends IntegrationTestSupport {

	@BeforeClass
	public static void beforeClass() throws Exception {
		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		vaultRule.prepare().getVaultOperations().write("secret/wintermute",
			Collections.singletonMap("vault.value", "spring.application.name value"));
		vaultRule.prepare().getVaultOperations()
			.write("secret/wintermute/integrationtest",
				Collections.singletonMap("vault.value",
					"spring.application.name:integrationtest value"));
		vaultRule.prepare().getVaultOperations().write("secret/neuromancer",
			Collections.singletonMap("vault.value",	"spring.cloud.vault.applicationName value"));
		vaultRule.prepare().getVaultOperations()
			.write("secret/neuromancer/integrationtest",
				Collections.singletonMap("vault.value",
					"spring.cloud.vault.applicationName:integrationtest value"));
		vaultRule.prepare().getVaultOperations()
			.write("secret/icebreaker",
				Collections.singletonMap("icebreaker.value",
					"additional context value"));
		vaultRule.prepare().getVaultOperations()
			.write("secret/icebreaker/integrationtest",
				Collections.singletonMap("icebreaker.value",
					"additional context:integrationtest value"));
	}

	@Value("${vault.value}")
	String configValue;

	@Value("${icebreaker.value}")
	String additionalValue;

	@Test
	public void getsSecretFromVaultUsingVaultApplicationName() {
		assertThat(configValue)
			.isEqualTo("spring.cloud.vault.applicationName:integrationtest value");
	}

	@Test
	public void getsSecretFromVaultUsingAdditionalContext() {
		assertThat(additionalValue)
			.isEqualTo("additional context:integrationtest value");
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication
				.run(VaultConfigWithContextTests.TestApplication.class, args);
		}
	}

}
