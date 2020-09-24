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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for Vault using the AWS integration.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.aws")
public class VaultAwsProperties implements VaultSecretBackendDescriptor {

	/**
	 * Enable aws backend usage.
	 */
	private boolean enabled;

	/**
	 * Role name for credentials.
	 */
	@Nullable
	private String role;

	/**
	 * aws backend path.
	 */
	private String backend = "aws";

	/**
	 * Target property for the obtained access key.
	 */
	private String accessKeyProperty = "cloud.aws.credentials.accessKey";

	/**
	 * Target property for the obtained secret key.
	 */
	private String secretKeyProperty = "cloud.aws.credentials.secretKey";

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Nullable
	public String getRole() {
		return this.role;
	}

	public void setRole(@Nullable String role) {
		this.role = role;
	}

	@Override
	public String getBackend() {
		return this.backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public String getAccessKeyProperty() {
		return this.accessKeyProperty;
	}

	public void setAccessKeyProperty(String accessKeyProperty) {
		this.accessKeyProperty = accessKeyProperty;
	}

	public String getSecretKeyProperty() {
		return this.secretKeyProperty;
	}

	public void setSecretKeyProperty(String secretKeyProperty) {
		this.secretKeyProperty = secretKeyProperty;
	}

}
