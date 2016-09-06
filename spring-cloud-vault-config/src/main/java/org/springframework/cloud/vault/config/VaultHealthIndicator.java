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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.vault.VaultHealthResponse;
import org.springframework.cloud.vault.VaultTemplate;

/**
 * @author Stuart Ingram
 */
public class VaultHealthIndicator implements HealthIndicator {

	@Autowired
	private VaultTemplate vaultTemplate;

	@Override
	public Health health() {

		try {

			VaultHealthResponse vaultHealthResponse = vaultTemplate.health();

			if (!vaultHealthResponse.isInitialized()) {
				return Health.down().withDetail("state", "Vault uninitialized").build();
			}

			if (vaultHealthResponse.isSealed()) {
				return Health.down().withDetail("state", "Vault sealed").build();
			}

			if (vaultHealthResponse.isStandby()) {
				return Health.outOfService().withDetail("state", "Vault in standby").build();
			}

			return Health.up().build();
		}
		catch (Exception e) {
			return Health.down().build();
		}
	}

}
