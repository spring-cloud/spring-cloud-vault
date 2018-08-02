/*
 * Copyright 2018 the original author or authors.
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

import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthIndicatorConfiguration;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.ReactiveVaultOperations;

/**
 * Configuration for {@link VaultReactiveHealthIndicator}.
 *
 * @author Mark Paluch
 * @since 2.0
 */
@Configuration
@ConditionalOnClass(Flux.class)
@ConditionalOnBean(ReactiveVaultOperations.class)
class VaultReactiveHealthIndicatorConfiguration
		extends
		CompositeReactiveHealthIndicatorConfiguration<VaultReactiveHealthIndicator, ReactiveVaultOperations> {

	private final Map<String, ReactiveVaultOperations> reactiveVaultTemplates;

	public VaultReactiveHealthIndicatorConfiguration(
			Map<String, ReactiveVaultOperations> reactiveVaultTemplates) {
		this.reactiveVaultTemplates = reactiveVaultTemplates;
	}

	@Bean
	@ConditionalOnMissingBean(name = { "vaultReactiveHealthIndicator" })
	public ReactiveHealthIndicator vaultReactiveHealthIndicator() {
		return this.createHealthIndicator(this.reactiveVaultTemplates);
	}
}
