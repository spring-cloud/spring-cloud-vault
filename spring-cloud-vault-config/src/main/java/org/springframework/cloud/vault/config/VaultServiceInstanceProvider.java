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

import org.springframework.cloud.client.ServiceInstance;

/**
 * Provider interface to obtain a {@link ServiceInstance} to look up the Vault service.
 *
 * @author Mark Paluch
 * @since 1.1
 */
@FunctionalInterface
public interface VaultServiceInstanceProvider {

	/**
	 * Lookup {@link ServiceInstance} by {@code serviceId}.
	 *
	 * @param serviceId the service Id.
	 * @return {@link ServiceInstance} for the given {@code serviceId}.
	 * @throws IllegalStateException if no service with {@code serviceId} was found.
	 */
	ServiceInstance getVaultServerInstance(String serviceId);
}
