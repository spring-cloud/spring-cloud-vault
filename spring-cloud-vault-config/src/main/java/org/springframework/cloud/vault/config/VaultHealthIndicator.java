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

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultHealth;

/**
 * Simple health indicator reporting Vault's availability.
 *
 * @author Stuart Ingram
 * @author Mark Paluch
 */
public class VaultHealthIndicator implements HealthIndicator {

	private final VaultOperations vaultOperations;

	public VaultHealthIndicator(VaultOperations vaultOperations) {
		this.vaultOperations = vaultOperations;
	}

	@Override
	public Health health() {

		try {

			VaultHealth vaultHealthResponse = vaultOperations.opsForSys().health();

			Builder healthBuilder = getHealthBuilder(vaultHealthResponse);

			if (StringUtils.hasText(vaultHealthResponse.getVersion())) {
				healthBuilder = healthBuilder.withDetail("version",
						vaultHealthResponse.getVersion());
			}

			return healthBuilder.build();
		}
		catch (Exception e) {
			return Health.down(e).build();
		}
	}

	private Builder getHealthBuilder(VaultHealth vaultHealthResponse) {

		if (!vaultHealthResponse.isInitialized()) {
			return Health.down().withDetail("state", "Vault uninitialized");
		}

		if (vaultHealthResponse.isSealed()) {
			return Health.down().withDetail("state", "Vault sealed");
		}

		if (vaultHealthResponse.isStandby()) {
			return Health.up().withDetail("state", "Vault in standby");
		}

		return Health.up();
	}
}
