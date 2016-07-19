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

package org.springframework.cloud.vault;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.cloud.vault.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Integration tests for {@link VaultClient} using AWS-EC2 login. This test requires AWS
 * credentials, a region and an AMI, see {@link #AWS_ACCESS_KEY}, {@link #AWS_SECRET_KEY}
 * and the {@link IntegrationTest} properties to be provided externally. It needs to be
 * run on a AWS-EC2 instance to be able to obtain instance metadata.
 *
 * @author Mark Paluch
 */
public class AwsEc2AuthenticationMethodsIntegrationTests extends AbstractIntegrationTests {

	private final static String AWS_REGION = "eu-west-1";
	private final static String AWS_AMI = "ami-f95ef58a";
	private final static String AWS_ACCESS_KEY = System.getProperty("aws.access.key");
	private final static String AWS_SECRET_KEY = System.getProperty("aws.secret.key");

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() throws Exception {

		assumeTrue(StringUtils.hasText(AWS_ACCESS_KEY)
				&& StringUtils.hasText(AWS_SECRET_KEY));

		if (!prepare().hasAuth("aws-ec2")) {
			prepare().mountAuth("aws-ec2");
		}

		Map<String, String> config = new HashMap<>();
		config.put("access_key", AWS_ACCESS_KEY);
		config.put("secret_key", AWS_SECRET_KEY);
		config.put("endpoint", String.format("https://ec2.%s.amazonaws.com", AWS_REGION));

		prepare().write("auth/aws-ec2/config/client", config);

		prepare().write(String.format("auth/aws-ec2/role/%s", AWS_AMI),
				Collections.singletonMap("bound_ami_id", AWS_AMI));
	}

	@Test
	public void loginShouldCreateAToken() throws Exception {

		VaultProperties vaultProperties = prepareAwsEc2Authentication();

		ClientAuthentication clientAuthentication = new DefaultClientAuthentication(
				vaultProperties, prepare().newVaultClient());

		assertThat(clientAuthentication.login()).isNotNull();
	}

	private VaultProperties prepareAwsEc2Authentication() {

		VaultProperties vaultProperties = Settings.createVaultProperties();
		vaultProperties.setAuthentication(AuthenticationMethod.AWS_EC2);

		return vaultProperties;
	}
}
