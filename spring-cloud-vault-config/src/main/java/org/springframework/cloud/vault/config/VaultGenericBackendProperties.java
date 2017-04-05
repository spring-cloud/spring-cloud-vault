/*
 * Copyright 2016-2017 the original author or authors.
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

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the generic backend.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.generic")
@Data
@Validated
public class VaultGenericBackendProperties implements EnvironmentAware {

	/**
	 * Enable the generic backend.
	 */
	private boolean enabled = true;

	/**
	 * Name of the default backend.
	 */
	@NotEmpty
	private String backend = "secret";

	/**
	 * Name of the default context.
	 */
	@NotEmpty
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

	@Override
	public void setEnvironment(Environment environment) {

		RelaxedPropertyResolver springCloudVaultPropertyResolver = new RelaxedPropertyResolver(
				environment, "spring.cloud.vault.");
		String springCloudVaultAppName = springCloudVaultPropertyResolver
				.getProperty("application-name");

		if (StringUtils.hasText(springCloudVaultAppName)) {
			this.applicationName = springCloudVaultAppName;
		}
		else {
			RelaxedPropertyResolver springPropertyResolver = new RelaxedPropertyResolver(
					environment, "spring.application.");
			String springAppName = springPropertyResolver.getProperty("name");

			if (StringUtils.hasText(springAppName)) {
				this.applicationName = springAppName;
			}
		}
	}
}
