/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.vault.config.databases;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the MongoDB integration.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.mongodb")
@Validated
public class VaultMongoProperties implements DatabaseSecretProperties {

	/**
	 * Enable mongodb backend usage.
	 */
	private boolean enabled = false;

	/**
	 * Role name for credentials.
	 */
	private String role;

	/**
	 * Cassandra backend path.
	 */
	@NotEmpty
	private String backend = "mongodb";

	/**
	 * Target property for the obtained username.
	 */
	@NotEmpty
	private String usernameProperty = "spring.data.mongodb.username";

	/**
	 * Target property for the obtained password.
	 */
	@NotEmpty
	private String passwordProperty = "spring.data.mongodb.password";

	/**
	 * Enable static role usage.
	 */
	private boolean staticRole = false;

	public VaultMongoProperties() {
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getRole() {
		return this.role;
	}

	public String getBackend() {
		return this.backend;
	}

	public String getUsernameProperty() {
		return this.usernameProperty;
	}

	public String getPasswordProperty() {
		return this.passwordProperty;
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

	public void setUsernameProperty(String usernameProperty) {
		this.usernameProperty = usernameProperty;
	}

	public void setPasswordProperty(String passwordProperty) {
		this.passwordProperty = passwordProperty;
	}

}
