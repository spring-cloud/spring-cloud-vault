/*
 * Copyright 2016-2020 the original author or authors.
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

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the AWS integration.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.aws")
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

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getRole() {
		return this.role;
	}

	public String getBackend() {
		return this.backend;
	}

	public String getAccessKeyProperty() {
		return this.accessKeyProperty;
	}

	public String getSecretKeyProperty() {
		return this.secretKeyProperty;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public void setAccessKeyProperty(String accessKeyProperty) {
		this.accessKeyProperty = accessKeyProperty;
	}

	public void setSecretKeyProperty(String secretKeyProperty) {
		this.secretKeyProperty = secretKeyProperty;
	}

}
