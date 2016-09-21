/*
 * Copyright 2016 the original author or authors.
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
 * Factory to convert {@link VaultSecretBackend} instance to a
 * {@link SecureBackendAccessor}.
 *
 * @author Mark Paluch
 */
public interface SecureBackendAccessorFactory<T extends VaultSecretBackend> {

	/**
	 * Converts a {@link VaultSecretBackend} into a {@link SecureBackendAccessor}.
	 * @param configurationProperties
	 * @return the {@link SecureBackendAccessor}.
	 */
	SecureBackendAccessor createSecureBackendAccessor(T configurationProperties);

	/**
	 * Checks whether the {@link VaultSecretBackend} is supported by this
	 * {@link SecureBackendAccessorFactory}.
	 * @param secretBackend must not be {@literal null}.
	 * @return {@literal true} if the given {@link VaultSecretBackend} is supported
	 */
	boolean supports(VaultSecretBackend secretBackend);
}
