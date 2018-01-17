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
 * Strategy interface to create {@link SecretBackendMetadata} from
 * {@link VaultSecretBackendDescriptor} properties.
 *
 * <p>
 * Classes implementing this interface must implement
 * {@link #supports(VaultSecretBackendDescriptor)} to determine whether a particular
 * {@link VaultSecretBackendDescriptor} is supported by this implementation. If a
 * {@link VaultSecretBackendDescriptor} instance is supported by the implementation, it
 * must be able to create {@link SecretBackendMetadata}, see
 * {@link #createMetadata(VaultSecretBackendDescriptor)}.
 *
 * <p>
 * Typically implemented by secret backend providers that implement access to a particular
 * backend using read operations.
 *
 * @author Mark Paluch
 * @see SecretBackendMetadata
 * @see LeasingSecretBackendMetadata
 * @see VaultSecretBackendDescriptor
 */
public interface SecretBackendMetadataFactory<T extends VaultSecretBackendDescriptor> {

	/**
	 * Converts a {@link VaultSecretBackendDescriptor} into a
	 * {@link SecretBackendMetadata}.
	 *
	 * @param backendDescriptor must not be {@literal null}.
	 * @return the {@link SecretBackendMetadata}.
	 * @see LeasingSecretBackendMetadata
	 */
	SecretBackendMetadata createMetadata(T backendDescriptor);

	/**
	 * Checks whether the {@link VaultSecretBackendDescriptor} is supported by this
	 * {@link SecretBackendMetadataFactory}.
	 *
	 * @param backendDescriptor must not be {@literal null}.
	 * @return {@literal true} if the given {@link VaultSecretBackendDescriptor} is
	 * supported.
	 */
	boolean supports(VaultSecretBackendDescriptor backendDescriptor);
}
