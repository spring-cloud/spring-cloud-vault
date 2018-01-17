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

package org.springframework.cloud.vault.config;

/**
 * Interface to be implemented by objects that describe a Vault secret backend.
 *
 * <p>
 * Typically used by {@link SecretBackendMetadataFactory} to provide path and
 * configuration to create a {@link SecretBackendMetadata} object.
 *
 * @author Mark Paluch
 * @see SecretBackendMetadataFactory
 * @see SecretBackendMetadata
 */
public interface VaultSecretBackendDescriptor {

	/**
	 * Backend path without leading/trailing slashes.
	 *
	 * @return the backend path such as {@code secret} or {@code mysql}.
	 */
	String getBackend();

	/**
	 * @return {@literal true} if the backend is enabled.
	 */
	boolean isEnabled();
}
