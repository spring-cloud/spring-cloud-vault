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

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.aws.VaultConfigAwsBootstrapConfiguration.AwsSecretBackendMetadataFactory.*;

/**
 * Integration tests for {@link VaultConfigTemplate} using the aws secret backend. This
 * test requires AWS credentials and a region, see {@link #AWS_ACCESS_KEY} and
 * {@link #AWS_SECRET_KEY} to be provided externally.
 *
 * @author Mark Paluch
 */
public class AwsSecretIntegrationTests extends IntegrationTestSupport {

	private final static String AWS_REGION = "eu-west-1";
	private final static String AWS_ACCESS_KEY = System.getProperty("aws.access.key");
	private final static String AWS_SECRET_KEY = System.getProperty("aws.secret.key");

	private final static String ARN = "arn:aws:iam::aws:policy/ReadOnlyAccess";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	private VaultAwsProperties aws = new VaultAwsProperties();

	/**
	 * Initialize the aws secret backend.
	 */
	@Before
	public void setUp() {

		assumeTrue(StringUtils.hasText(AWS_ACCESS_KEY)
				&& StringUtils.hasText(AWS_SECRET_KEY));

		aws.setEnabled(true);
		aws.setRole("readonly");

		if (!prepare().hasSecretBackend(aws.getBackend())) {
			prepare().mountSecret(aws.getBackend());
		}

		VaultOperations vaultOperations = prepare().getVaultOperations();

		Map<String, String> connection = new HashMap<>();
		connection.put("region", AWS_REGION);
		connection.put("access_key", AWS_ACCESS_KEY);
		connection.put("secret_key", AWS_SECRET_KEY);

		vaultOperations.write(String.format("%s/config/root", aws.getBackend()),
				connection);

		vaultOperations.write(
				String.format("%s/roles/%s", aws.getBackend(), aws.getRole()),
				Collections.singletonMap("arn", ARN));

		configOperations = new VaultConfigTemplate(vaultOperations, vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = configOperations.read(forAws(aws))
				.getData();

		assertThat(secretProperties).containsKeys("cloud.aws.credentials.accessKey",
				"cloud.aws.credentials.secretKey");
	}
}
