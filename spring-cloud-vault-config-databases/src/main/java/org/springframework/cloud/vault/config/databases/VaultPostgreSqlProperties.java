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
package org.springframework.cloud.vault.config.databases;

import javax.validation.constraints.NotEmpty;

import lombok.Data;
import lombok.Getter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the PostgreSQL integration.
 *
 * @author Mark Paluch
 * @deprecated since 2.0. Use {@link VaultDatabaseProperties}.
 */
@ConfigurationProperties("spring.cloud.vault.postgresql")
@Data
@Validated
@Deprecated
public class VaultPostgreSqlProperties implements DatabaseSecretProperties {

	/**
	 * Enable postgresql backend usage.
	 */
	@Deprecated
	@Getter(onMethod_ = { @DeprecatedConfigurationProperty(reason = "Use spring.cloud.vault.database") })
	private boolean enabled = false;

	/**
	 * Role name for credentials.
	 */
	@Getter(onMethod_ = { @DeprecatedConfigurationProperty(reason = "Use spring.cloud.vault.database") })
	private String role;

	/**
	 * postgresql backend path.
	 */
	@NotEmpty
	private String backend = "postgresql";

	/**
	 * Target property for the obtained username.
	 */
	@NotEmpty
	private String usernameProperty = "spring.datasource.username";

	/**
	 * Target property for the obtained username.
	 */
	@NotEmpty
	private String passwordProperty = "spring.datasource.password";
}
