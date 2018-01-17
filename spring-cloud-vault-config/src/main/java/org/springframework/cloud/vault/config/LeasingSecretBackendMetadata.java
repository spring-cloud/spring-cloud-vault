/*
 * Copyright 2017-2018 the original author or authors.
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
	 * <p/>
	 * Lease mode is considered only by lease-aware property sources.
	 *
	 * @return the lease mode of this secret backend.
	 * @since 1.1
	 */
	Mode getLeaseMode();
}
