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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.Files;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultMount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.vault.util.Settings.findWorkDir;

/**
 * Integration test using config infrastructure with TLS certificate authentication. In
 * case this test should fail because of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * @author Quincy Conduff
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VaultConfigTlsCertAuthenticationMountPathTests.TestApplication.class, properties = {
		"spring.cloud.vault.authentication=CERT", "spring.cloud.vault.ssl.key-store=file:../work/client-cert.jks",
		"spring.cloud.vault.ssl.key-store-password=changeit", "spring.cloud.vault.ssl.cert-auth-path=nonstandard",
		"spring.cloud.vault.application-name=VaultConfigTlsCertAuthenticationMountPathTests",
		"spring.cloud.vault.reactive.enabled=false", "spring.cloud.bootstrap.enabled=true" })
public class VaultConfigTlsCertAuthenticationMountPathTests {

	@Value("${vault.value}")
	String configValue;

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		VaultProperties vaultProperties = Settings.createVaultProperties();

		vaultProperties.getSsl().setCertAuthPath("nonstandard");

		if (!vaultRule.prepare().hasAuth(vaultProperties.getSsl().getCertAuthPath())) {
			vaultRule.prepare()
				.getVaultOperations()
				.opsForSys()
				.authMount(vaultProperties.getSsl().getCertAuthPath(), VaultMount.builder().type("cert").build());
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		String rules = "path \"*\" {\n    capabilities = [\"read\"]\n}";

		vaultOperations.write("sys/policy/testpolicy", Collections.singletonMap("policy", rules));

		vaultOperations.write("secret/" + VaultConfigTlsCertAuthenticationMountPathTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		File workDir = findWorkDir();

		String certificate = Files.contentOf(new File(workDir, "ca/certs/client.cert.pem"), StandardCharsets.US_ASCII);

		Map<String, String> role = new HashMap<>();
		role.put("certificate", certificate);
		role.put("policies", "testpolicy");

		vaultOperations.write(String.join("/", "auth", vaultProperties.getSsl().getCertAuthPath(), "certs", "my-role"),
				role);
	}

	@Test
	public void contextLoads() {
		assertThat(this.configValue).isEqualTo("foo");
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

}
