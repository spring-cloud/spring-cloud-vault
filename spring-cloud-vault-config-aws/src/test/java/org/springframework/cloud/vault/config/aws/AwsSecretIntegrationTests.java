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
package org.springframework.cloud.vault.config.aws;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;
import static org.springframework.cloud.vault.config.aws.VaultConfigAwsBootstrapConfiguration.AwsSecureBackendAccessorFactory.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.vault.AbstractIntegrationTests;
import org.springframework.cloud.vault.TestRestTemplateFactory;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.util.StringUtils;

/**
 * Integration tests for {@link VaultClient} using the aws secret backend. This test
 * requires AWS credentials and a region, see {@link #AWS_ACCESS_KEY} and
 * {@link #AWS_SECRET_KEY} to be provided externally.
 *
 * @author Mark Paluch
 */
public class AwsSecretIntegrationTests extends AbstractIntegrationTests {

	private final static String AWS_REGION = "eu-west-1";
	private final static String AWS_ACCESS_KEY = System.getProperty("aws.access.key");
	private final static String AWS_SECRET_KEY = System.getProperty("aws.secret.key");

	private final static String ARN = "arn:aws:iam::aws:policy/ReadOnlyAccess";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultClient vaultClient = new VaultClient(vaultProperties);
	private VaultAwsProperties aws = new VaultAwsProperties();

	/**
	 * Initialize the aws secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(StringUtils.hasText(AWS_ACCESS_KEY)
				&& StringUtils.hasText(AWS_SECRET_KEY));

		aws.setEnabled(true);
		aws.setRole("readonly");

		if (!prepare().hasSecret(aws.getBackend())) {
			prepare().mountSecret(aws.getBackend());
		}

		Map<String, String> connection = new HashMap<>();
		connection.put("region", AWS_REGION);
		connection.put("access_key", AWS_ACCESS_KEY);
		connection.put("secret_key", AWS_SECRET_KEY);

		prepare().write(String.format("%s/config/root", aws.getBackend()), connection);

		prepare().write(String.format("%s/roles/%s", aws.getBackend(), aws.getRole()),
				Collections.singletonMap("arn", ARN));

		vaultClient.setRest(TestRestTemplateFactory.create(vaultProperties));
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = vaultClient.read(forAws(aws),
				Settings.token());

		assertThat(secretProperties).containsKeys("cloud.aws.credentials.accessKey",
				"cloud.aws.credentials.secretKey");
	}

}
