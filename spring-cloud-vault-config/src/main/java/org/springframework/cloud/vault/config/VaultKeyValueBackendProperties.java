/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.vault.config;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the key-value backend.
 *
 * @author Mark Paluch
 * @since 2.0
 */
@ConfigurationProperties("spring.cloud.vault.kv")
@Data
@Validated
public class VaultKeyValueBackendProperties implements EnvironmentAware,
		VaultKeyValueBackendPropertiesSupport {

	/**
	 * Enable the kev-value backend.
	 */
	private boolean enabled = false;

	/**
	 * Name of the default backend.
	 */
	@NotEmpty
	private String backend = "secret";

	/**
	 * Name of the default context.
	 */
	private String defaultContext = "application";

	/**
	 * Profile-separator to combine application name and profile.
	 */
	@NotEmpty
	private String profileSeparator = "/";

	/**
	 * Application name to be used for the context.
	 */
	private String applicationName = "application";

	/**
	 * Key-Value backend version. Currently supported versions are:
	 * <ul>
	 * <li>Version 1 (unversioned key-value backend).</li>
	 * <li>Version 2 (versioned key-value backend).</li>
	 * </ul>
	 */
	private int backendVersion = 2;

	@Override
	public void setEnvironment(Environment environment) {

		String springCloudVaultAppName = environment
				.getProperty("spring.cloud.vault.application-name");

		if (StringUtils.hasText(springCloudVaultAppName)) {
			this.applicationName = springCloudVaultAppName;
		}
		else {
			String springAppName = environment.getProperty("spring.application.name");

			if (StringUtils.hasText(springAppName)) {
				this.applicationName = springAppName;
			}
		}
	}
}
