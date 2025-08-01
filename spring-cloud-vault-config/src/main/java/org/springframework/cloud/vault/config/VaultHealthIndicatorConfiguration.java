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

import java.util.Map;

import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultOperations;

/**
 * Configuration for {@link VaultHealthIndicator}.
 *
 * @author Stuart Ingram
 * @author Mark Paluch
 * @since 1.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(VaultOperations.class)
class VaultHealthIndicatorConfiguration
		extends CompositeHealthContributorConfiguration<VaultHealthIndicator, VaultOperations> {

	private final Map<String, VaultOperations> vaultTemplates;

	VaultHealthIndicatorConfiguration(Map<String, VaultOperations> vaultTemplates) {
		super(VaultHealthIndicator::new);
		this.vaultTemplates = vaultTemplates;
	}

	@Bean
	@ConditionalOnMissingBean(name = { "vaultHealthIndicator" })
	public HealthContributor vaultHealthIndicator() {
		return createContributor(this.vaultTemplates);
	}

}
