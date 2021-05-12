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

package org.springframework.cloud.vault.config.aws;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for Vault using the AWS integration.
 *
 * @author Mark Paluch
 * @author Kris Iyer
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
	 * aws credential type
	 */
	private AwsCredentialType credentialType = AwsCredentialType.IAM_USER;

	/**
	 * Target property for the obtained access key.
	 */
	private String accessKeyProperty = "cloud.aws.credentials.accessKey";

	/**
	 * Target property for the obtained secret key.
	 */
	private String secretKeyProperty = "cloud.aws.credentials.secretKey";

	/**
	 * Target property for the obtained secret key.
	 */
	private String sessionTokenKeyProperty = "cloud.aws.credentials.sessionToken";

	/**
	 * Role arn for assumed_role in case we have multiple roles associated with the vault
	 * role.
	 * @since 3.0.2
	 */
	@Nullable
	private String roleArn;

	/**
	 * TTL for sts tokens. Defaults to whatever the vault Role may have for Max. Also
	 * limited to what AWS supports to be the max for STS.
	 * @since 3.0.2
	 */
	private Duration ttl = Duration.ZERO;

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

	public AwsCredentialType getCredentialType() {
		return this.credentialType;
	}

	public void setCredentialType(AwsCredentialType credentialType) {
		this.credentialType = credentialType;
	}

	public String getSessionTokenKeyProperty() {
		return this.sessionTokenKeyProperty;
	}

	public void setSessionTokenKeyProperty(String sessionTokenKeyProperty) {
		this.sessionTokenKeyProperty = sessionTokenKeyProperty;
	}

	@Nullable
	public String getRoleArn() {
		return this.roleArn;
	}

	public void setRoleArn(String roleArn) {
		this.roleArn = roleArn;
	}

	public Duration getTtl() {
		return this.ttl;
	}

	public void setTtl(Duration ttl) {
		this.ttl = ttl;
	}

}
