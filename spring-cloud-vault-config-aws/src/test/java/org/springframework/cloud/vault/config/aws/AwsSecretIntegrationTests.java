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

package org.springframework.cloud.vault.config.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.aws.VaultConfigAwsBootstrapConfiguration.AwsSecretBackendMetadataFactory.forAws;

/**
 * Integration tests for {@link VaultConfigTemplate} using the aws secret backend. This
 * test requires AWS credentials and a region, see {@link #AWS_ACCESS_KEY} and
 * {@link #AWS_SECRET_KEY} to be provided externally.
 *
 * @author Mark Paluch
 */
// FIXME: 4.0.0
@Disabled("NoClassDefFoundError: org/springframework/http/client/Netty4ClientHttpRequestFactory")
public class AwsSecretIntegrationTests extends IntegrationTestSupport {

	private static final String AWS_REGION = "eu-west-1";

	private static final String AWS_ACCESS_KEY = System.getProperty("aws.access.key");

	private static final String AWS_SECRET_KEY = System.getProperty("aws.secret.key");

	private static final String ARN = "arn:aws:iam::aws:policy/ReadOnlyAccess";

	private VaultProperties vaultProperties = Settings.createVaultProperties();

	private VaultConfigOperations configOperations;

	private VaultAwsProperties aws = new VaultAwsProperties();

	/**
	 * Initialize the aws secret backend.
	 */
	@BeforeEach
	public void setUp() {

		assumeTrue(StringUtils.hasText(AWS_ACCESS_KEY) && StringUtils.hasText(AWS_SECRET_KEY));

		this.aws.setEnabled(true);
		this.aws.setRole("readonly");

		if (!prepare().hasSecretBackend(this.aws.getBackend())) {
			prepare().mountSecret(this.aws.getBackend());
		}

		VaultOperations vaultOperations = prepare().getVaultOperations();

		Map<String, String> connection = new HashMap<>();
		connection.put("region", AWS_REGION);
		connection.put("access_key", AWS_ACCESS_KEY);
		connection.put("secret_key", AWS_SECRET_KEY);

		vaultOperations.write(String.format("%s/config/root", this.aws.getBackend()), connection);

		vaultOperations.write(String.format("%s/roles/%s", this.aws.getBackend(), this.aws.getRole()),
				Collections.singletonMap("arn", ARN));

		this.configOperations = new VaultConfigTemplate(vaultOperations, this.vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() {

		Map<String, Object> secretProperties = this.configOperations.read(forAws(this.aws)).getData();

		assertThat(secretProperties).containsKeys("cloud.aws.credentials.accessKey", "cloud.aws.credentials.secretKey");
	}

}
