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

import javax.validation.constraints.NotEmpty;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the AWS integration.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.aws")
@Data
@Validated
public class VaultAwsProperties implements VaultSecretBackendDescriptor {

	/**
	 * Enable aws backend usage.
	 */
	private boolean enabled = false;

	/**
	 * Role name for credentials.
	 */
	private String role;

	/**
	 * aws backend path.
	 */
	@NotEmpty
	private String backend = "aws";

	/**
	 * Target property for the obtained access key.
	 */
	@NotEmpty
	private String accessKeyProperty = "cloud.aws.credentials.accessKey";

	/**
	 * Target property for the obtained secret key.
	 */
	@NotEmpty
	private String secretKeyProperty = "cloud.aws.credentials.secretKey";
}
