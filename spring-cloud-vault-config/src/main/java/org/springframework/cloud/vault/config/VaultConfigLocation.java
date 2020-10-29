/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Vault-specific implementation for a {@link ConfigDataLocation}. Consists of a
 * {@link SecretBackendMetadata}.
 *
 * @author Mark Paluch
 * @since 3.0
 * @see SecretBackendMetadata
 */
public class VaultConfigLocation extends ConfigDataResource {

	/**
	 * Prefix used to indicate a {@link VaultConfigLocation}.
	 */
	public static final String VAULT_PREFIX = "vault:";

	private final SecretBackendMetadata secretBackendMetadata;

	private final boolean optional;

	public VaultConfigLocation(String contextPath, boolean optional) {

		Assert.hasText(contextPath, "Context path must not be empty");

		this.secretBackendMetadata = KeyValueSecretBackendMetadata.create(contextPath);
		this.optional = optional;
	}

	public VaultConfigLocation(SecretBackendMetadata secretBackendMetadata, boolean optional) {

		Assert.notNull(secretBackendMetadata, "SecretBackendMetadata must not be null");

		this.secretBackendMetadata = secretBackendMetadata;
		this.optional = optional;
	}

	public SecretBackendMetadata getSecretBackendMetadata() {
		return this.secretBackendMetadata;
	}

	public boolean isOptional() {
		return this.optional;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VaultConfigLocation)) {
			return false;
		}
		VaultConfigLocation that = (VaultConfigLocation) o;
		if (this.optional != that.optional) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.secretBackendMetadata.getName(), that.secretBackendMetadata.getName())
				&& ObjectUtils.nullSafeEquals(this.secretBackendMetadata.getPath(),
						that.secretBackendMetadata.getPath());
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.secretBackendMetadata.getName());
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.secretBackendMetadata.getPath());
		result = 31 * result + (this.optional ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName());
		sb.append(" [path='").append(this.secretBackendMetadata.getPath()).append('\'');
		sb.append(", optional=").append(this.optional);
		sb.append(']');
		return sb.toString();
	}

}
