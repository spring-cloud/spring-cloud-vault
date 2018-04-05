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
package org.springframework.cloud.vault.config.aws;

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
import org.springframework.cloud.vault.util.VaultRule;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

/**
 * Integration tests using the aws secret backend. In case this test should fail because
 * of SSL make sure you run the test within the
 * spring-cloud-vault-config/spring-cloud-vault-config directory as the keystore is
 * referenced with {@code ../work/keystore.jks}.
 *
 * <p>
 * This test requires AWS credentials and a region, see {@link #AWS_ACCESS_KEY},
 * {@link #AWS_SECRET_KEY} and the {@link SpringBootTest} properties to be provided
 * externally.
 *
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = VaultConfigAwsTests.TestApplication.class, properties = {
		"spring.cloud.vault.aws.enabled=true", "spring.cloud.vault.aws.role=readonly",
		"cloud.aws.region.auto=false", "cloud.aws.region.static=eu-west-1" })
public class VaultConfigAwsTests {

	private final static String AWS_REGION = "eu-west-1";
	private final static String AWS_ACCESS_KEY = System.getProperty("aws.access.key");
	private final static String AWS_SECRET_KEY = System.getProperty("aws.secret.key");

	private final static String ARN = "arn:aws:iam::aws:policy/ReadOnlyAccess";

	/**
	 * Initialize the aws secret backend.
	 */
	@BeforeClass
	public static void beforeClass() {

		assumeTrue(StringUtils.hasText(AWS_ACCESS_KEY)
				&& StringUtils.hasText(AWS_SECRET_KEY));

		VaultRule vaultRule = new VaultRule();
		vaultRule.before();

		if (!vaultRule.prepare().hasSecretBackend("aws")) {
			vaultRule.prepare().mountSecret("aws");
		}

		VaultOperations vaultOperations = vaultRule.prepare().getVaultOperations();

		Map<String, String> connection = new HashMap<>();
		connection.put("region", AWS_REGION);
		connection.put("access_key", AWS_ACCESS_KEY);
		connection.put("secret_key", AWS_SECRET_KEY);

		vaultOperations.write("aws/config/root", connection);

		vaultOperations.write("aws/roles/readonly", Collections.singletonMap("arn", ARN));
	}

	@Value("${cloud.aws.credentials.accessKey}")
	String accessKey;

	@Value("${cloud.aws.credentials.secretKey}")
	String secretKey;

	@Test
	public void shouldInitializeAwsProperties() {

		assertThat(accessKey).isNotEmpty();
		assertThat(secretKey).isNotEmpty();
	}

	@SpringBootApplication
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}
	}
}
