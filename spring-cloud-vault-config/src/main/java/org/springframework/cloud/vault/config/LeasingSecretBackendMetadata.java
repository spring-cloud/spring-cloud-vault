/*
 * Copyright 2017-2020 the original author or authors.
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

import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode;

/**
 * Lease extension to {@link SecretBackendMetadata} providing a
 * {@link org.springframework.vault.core.lease.domain.RequestedSecret.Mode lease mode}.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see org.springframework.vault.core.lease.domain.RequestedSecret
 */
public interface LeasingSecretBackendMetadata extends SecretBackendMetadata {

	/**
	 * Return the lease mode of this secret backend.
	 * <p>
	 * Lease mode is considered only by lease-aware property sources.
	 * @return the lease mode of this secret backend.
	 * @since 1.1
	 */
	Mode getLeaseMode();

	/**
	 * Callback method before registering a {@link RequestedSecret secret} with {@link SecretLeaseContainer}.
	 * Registering a {@code before} callback allows event consumption before the secrets are visible in the associated property source.
	 *
	 * @param secret the requested secret.
	 * @param container the lease container that was used to request the secret.
	 * @since 3.0
	 */
	default void beforeRegistration(RequestedSecret secret, SecretLeaseContainer container) {
	}

	/**
	 * Callback method after registering a {@link RequestedSecret secret} with {@link SecretLeaseContainer}.
	 * Registering a {@code after} callback allows event consumption after the secrets are visible in the associated property source.
	 * Note that this callback does not necessarily guarantee notification of the initial secrets retrieval.
	 *
	 * @param secret the requested secret.
	 * @param container the lease container that was used to request the secret.
	 * @since 3.0
	 */
	default void afterRegistration(RequestedSecret secret, SecretLeaseContainer container) {
	}

}
