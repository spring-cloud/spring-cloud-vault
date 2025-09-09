/*
 * Copyright 2018-present the original author or authors.
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

import org.springframework.boot.actuate.health.Health;
import org.springframework.util.StringUtils;
import org.springframework.vault.support.VaultHealth;

/**
 * Common delegate to transport health properties into the Health actuator
 * {@link Health.Builder}.
 *
 * @author Mark Paluch
 * @since 2.2
 */
final class HealthBuilderDelegate {

	private HealthBuilderDelegate() {
	}

	static void contributeToHealth(VaultHealth healthResponse, Health.Builder builder) {

		if (!healthResponse.isInitialized()) {
			builder.down().withDetail("state", "Vault uninitialized");
		}
		else if (healthResponse.isSealed()) {
			builder.down().withDetail("state", "Vault sealed");
		}
		else if (healthResponse.isStandby()) {
			builder.up().withDetail("state", "Vault in standby");
		}
		else if (healthResponse.isPerformanceStandby()) {
			builder.up().withDetail("state", "Vault in performance standby");
		}
		else if (healthResponse.isRecoveryReplicationSecondary()) {
			builder.up().withDetail("state", "Vault in recovery replication secondary mode");
		}
		else {
			builder.up();
		}

		if (StringUtils.hasText(healthResponse.getVersion())) {
			builder.withDetail("version", healthResponse.getVersion());
		}
	}

}
