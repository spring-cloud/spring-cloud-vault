/*
 * Copyright 2016-2020 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultSysOperations;
import org.springframework.vault.support.VaultHealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VaultHealthIndicator}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class VaultHealthIndicatorUnitTests {

	@Mock
	VaultOperations vaultOperations;

	@Mock
	VaultSysOperations vaultSysOperations;

	@Mock
	VaultHealth healthResponse;

	VaultHealthIndicator healthIndicator;

	@Before
	public void before() {

		this.healthIndicator = new VaultHealthIndicator(this.vaultOperations);

		when(this.vaultOperations.opsForSys()).thenReturn(this.vaultSysOperations);
		when(this.vaultSysOperations.health()).thenReturn(this.healthResponse);
	}

	@Test
	public void shouldReportHealthyService() {

		when(this.healthResponse.isInitialized()).thenReturn(true);
		when(this.vaultOperations.opsForSys()).thenReturn(this.vaultSysOperations);

		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

	@Test
	public void shouldReportSealedService() {

		when(this.healthResponse.isInitialized()).thenReturn(true);
		when(this.healthResponse.isSealed()).thenReturn(true);

		Health health = this.healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault sealed");
	}

	@Test
	public void shouldReportUninitializedService() {

		Health health = this.healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault uninitialized");
	}

	@Test
	public void shouldReportStandbyService() {

		when(this.healthResponse.isInitialized()).thenReturn(true);
		when(this.healthResponse.isStandby()).thenReturn(true);

		Health health = this.healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("state", "Vault in standby");
	}

	@Test
	public void exceptionsShouldReportDownStatus() {

		reset(this.vaultSysOperations);
		when(this.vaultSysOperations.health()).thenThrow(new IllegalStateException());

		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsKey("error");
	}

	@Test
	public void shouldReportPerformanceStandby() {

		when(this.healthResponse.isInitialized()).thenReturn(true);
		when(this.healthResponse.isPerformanceStandby()).thenReturn(true);

		Health health = this.healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("state", "Vault in performance standby");
	}

	@Test
	public void shouldReportRecoveryReplication() {

		when(this.healthResponse.isInitialized()).thenReturn(true);
		when(this.healthResponse.isRecoveryReplicationSecondary()).thenReturn(true);

		Health health = this.healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("state", "Vault in recovery replication secondary mode");
	}

}
