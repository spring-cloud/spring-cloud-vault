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

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
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
public class VaultHealthIndicator extends AbstractHealthIndicator {

	private final VaultOperations vaultOperations;

	public VaultHealthIndicator(VaultOperations vaultOperations) {
		this.vaultOperations = vaultOperations;
	}

	@Override
	protected void doHealthCheck(Builder builder) {

		VaultHealth vaultHealthResponse = vaultOperations.opsForSys().health();

		if (!vaultHealthResponse.isInitialized()) {
			builder.down().withDetail("state", "Vault uninitialized");
		}
		else

		if (vaultHealthResponse.isSealed()) {
			builder.down().withDetail("state", "Vault sealed");
		}
		else

		if (vaultHealthResponse.isStandby()) {
			builder.up().withDetail("state", "Vault in standby");
		}
		else {
			builder.up();
		}

		if (StringUtils.hasText(vaultHealthResponse.getVersion())) {
			builder.withDetail("version", vaultHealthResponse.getVersion());
		}
	}
}
