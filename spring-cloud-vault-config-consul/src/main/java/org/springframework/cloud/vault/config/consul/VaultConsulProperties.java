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

package org.springframework.cloud.vault.config.consul;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for HashiCorp Consul.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.consul")
public class VaultConsulProperties implements VaultSecretBackendDescriptor {

	/**
	 * Enable consul backend usage.
	 */
	private boolean enabled;

	/**
	 * Role name for credentials.
	 */
	@Nullable
	private String role;

	/**
	 * Consul backend path.
	 */
	private String backend = "consul";

	/**
	 * Target property for the obtained token.
	 */
	private String tokenProperty = "spring.cloud.consul.token";

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

	public String getTokenProperty() {
		return this.tokenProperty;
	}

	public void setTokenProperty(String tokenProperty) {
		this.tokenProperty = tokenProperty;
	}

}
