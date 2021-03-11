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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

/**
 * Provider for {@link ServiceInstance} to look up the Vault service.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class ReacvtiveDiscoveryClientVaultServiceInstanceProvider {

	private static final Log log = LogFactory.getLog(ReacvtiveDiscoveryClientVaultServiceInstanceProvider.class);

	private final ReactiveDiscoveryClient client;

	ReacvtiveDiscoveryClientVaultServiceInstanceProvider(ReactiveDiscoveryClient client) {
		this.client = client;
	}

	Mono<ServiceInstance> getVaultServerInstance(String serviceId) {

		log.debug("Locating Vault server (" + serviceId + ") via discovery");

		return this.client.getInstances(serviceId).collectList().handle((instances, sink) -> {

			if (instances.isEmpty()) {
				sink.error(new IllegalStateException("No instances found of Vault server (" + serviceId + ")"));
				return;
			}

			ServiceInstance instance = instances.get(0);

			log.debug("Located Vault server (" + serviceId + ") via discovery: " + instance);

			sink.next(instance);
		});
	}

}
