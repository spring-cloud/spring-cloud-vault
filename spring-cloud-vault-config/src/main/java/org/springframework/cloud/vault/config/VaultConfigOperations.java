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

import org.springframework.vault.core.VaultOperations;

/**
 * Interface that specified a basic set of Vault operations, implemented by
 * {@link VaultConfigTemplate}.
 *
 * @author Mark Paluch
 * @see VaultConfigTemplate
 * @see Secrets
 */
public interface VaultConfigOperations {

	/**
	 * Read secrets from a secret backend encapsulated within a
	 * {@link SecretBackendMetadata}. Reading data using this method is suitable for
	 * secret backends that do not require a request body.
	 *
	 * @param secretBackendMetadata must not be {@literal null}.
	 * @return the configuration data. May be empty but never {@literal null}.
	 * @throws IllegalStateException if {@link VaultProperties#isFailFast()} is enabled.
	 */
	Secrets read(SecretBackendMetadata secretBackendMetadata);

	/**
	 * @return the underlying {@link VaultOperations}.
	 */
	VaultOperations getVaultOperations();
}
