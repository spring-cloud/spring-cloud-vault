/*
 * Copyright 2017-2018 the original author or authors.
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
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.cloud.vault.util.Version;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Policy;
import org.springframework.vault.support.Policy.BuiltinCapabilities;
import org.springframework.vault.support.Policy.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.util.Settings.findWorkDir;

/**
 * Integration test using config infrastructure with Kubernetes authentication.
 *
 * @author Michal Budzyn
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigKubernetesTests.TestApplication.class, properties = {
		"spring.cloud.vault.authentication=kubernetes",
		"spring.cloud.vault.kubernetes.role=my-role",
		"spring.cloud.vault.kubernetes.service-account-token-file=../work/minikube/hello-minikube-token",
		"spring.cloud.vault.application-name=VaultConfigKubernetesTests" })
public class VaultConfigKubernetesTests {

	@Value("${vault.value}")
	String configValue;

	@BeforeClass
	public static void beforeClass() {

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		String minikubeIp = System.getProperty("MINIKUBE_IP");
		assumeTrue(StringUtils.hasText(minikubeIp)
				&& vaultRule.prepare().getVersion()
						.isGreaterThanOrEqualTo(Version.parse("0.8.3")));

		if (!vaultRule.prepare().hasAuth("kubernetes")) {
			vaultRule.prepare().mountAuth("kubernetes");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Policy policy = Policy.of(Rule.builder().path("*")
				.capabilities(BuiltinCapabilities.READ).build());

		vaultOperations.opsForSys().createOrUpdatePolicy("testpolicy", policy);

		vaultOperations.write(
				"secret/" + VaultConfigKubernetesTests.class.getSimpleName(),
				Collections.singletonMap("vault.value", "foo"));

		File workDir = findWorkDir();
		String certificate = Files.contentOf(new File(workDir, "minikube/ca.crt"),
				StandardCharsets.US_ASCII);

		String host = String.format("https://%s:8443", minikubeIp);
		Map<String, String> kubeConfig = new HashMap<>();
		kubeConfig.put("kubernetes_ca_cert", certificate);
		kubeConfig.put("kubernetes_host", host);
		vaultOperations.write("auth/kubernetes/config", kubeConfig);

		Map<String, String> roleData = new HashMap<>();
		roleData.put("bound_service_account_names", "default");
		roleData.put("bound_service_account_namespaces", "default");
		roleData.put("policies", "testpolicy");
		roleData.put("ttl", "1h");
		vaultOperations.write("auth/kubernetes/role/my-role", roleData);
	}

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
