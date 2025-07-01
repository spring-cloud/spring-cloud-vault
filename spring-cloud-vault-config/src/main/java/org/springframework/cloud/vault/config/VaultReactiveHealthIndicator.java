/*
 * Copyright 2018-2025 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Health.Builder;
import org.springframework.vault.core.ReactiveVaultOperations;

/**
 * Reactive health indicator reporting Vault's availability.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class VaultReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveVaultOperations vaultOperations;

	public VaultReactiveHealthIndicator(ReactiveVaultOperations vaultOperations) {
		this.vaultOperations = vaultOperations;
	}

	@Override
	protected Mono<Health> doHealthCheck(Builder builder) {

		return this.vaultOperations.opsForSys().health().map((vaultHealthResponse) -> {

			HealthBuilderDelegate.contributeToHealth(vaultHealthResponse, builder);
			return builder.build();
		});
	}

}
