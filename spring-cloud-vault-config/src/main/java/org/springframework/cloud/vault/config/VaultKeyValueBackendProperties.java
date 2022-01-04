/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.vault.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Vault using the key-value backend.
 *
 * @author Mark Paluch
 * @since 2.0
 */
@ConfigurationProperties(VaultKeyValueBackendProperties.PREFIX)
@Validated
public class VaultKeyValueBackendProperties implements EnvironmentAware, VaultKeyValueBackendPropertiesSupport {

	/**
	 * Configuration prefix for config properties.
	 */
	public static final String PREFIX = "spring.cloud.vault.kv";

	/**
	 * Enable the kev-value backend.
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
	 * List of active profiles.
	 * @since 3.0
	 */
	@Nullable
	private List<String> profiles;

	/**
	 * Key-Value backend version. Currently supported versions are:
	 * <ul>
	 * <li>Version 1 (unversioned key-value backend).</li>
	 * <li>Version 2 (versioned key-value backend).</li>
	 * </ul>
	 */
	private int backendVersion = 2;

	public VaultKeyValueBackendProperties() {
	}

	@Override
	public void setEnvironment(Environment environment) {

		String springCloudVaultAppName = environment.getProperty("spring.cloud.vault.application-name");

		if (StringUtils.hasText(springCloudVaultAppName)) {
			this.applicationName = springCloudVaultAppName;
		}
		else {
			String springAppName = environment.getProperty("spring.application.name");

			if (StringUtils.hasText(springAppName)) {
				this.applicationName = springAppName;
			}
		}

		if (this.profiles == null) {
			this.profiles = Arrays.asList(environment.getActiveProfiles());
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getBackend() {
		return this.backend;
	}

	public String getDefaultContext() {
		return this.defaultContext;
	}

	public String getProfileSeparator() {
		return this.profileSeparator;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	@Override
	public List<String> getProfiles() {
		if (this.profiles == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(this.profiles));
	}

	@Deprecated
	@DeprecatedConfigurationProperty(
			reason = "Backend version no longer required. The kv version is determined during secret retrieval")
	public int getBackendVersion() {
		return this.backendVersion;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public void setDefaultContext(String defaultContext) {
		this.defaultContext = defaultContext;
	}

	public void setProfileSeparator(String profileSeparator) {
		this.profileSeparator = profileSeparator;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setProfiles(List<String> profiles) {
		this.profiles = profiles;
	}

	public void setBackendVersion(int backendVersion) {
		this.backendVersion = backendVersion;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName());
		sb.append(" [enabled=").append(this.enabled);
		sb.append(", backend='").append(this.backend).append('\'');
		sb.append(", defaultContext='").append(this.defaultContext).append('\'');
		sb.append(", profileSeparator='").append(this.profileSeparator).append('\'');
		sb.append(", applicationName='").append(this.applicationName).append('\'');
		sb.append(", profiles='").append(this.profiles).append('\'');
		sb.append(", backendVersion=").append(this.backendVersion);
		sb.append(']');
		return sb.toString();
	}

}
