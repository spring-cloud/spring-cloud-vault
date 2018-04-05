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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

		healthIndicator = new VaultHealthIndicator(vaultOperations);

		when(vaultOperations.opsForSys()).thenReturn(vaultSysOperations);
		when(vaultSysOperations.health()).thenReturn(healthResponse);
	}

	@Test
	public void shouldReportHealthyService() {

		when(healthResponse.isInitialized()).thenReturn(true);
		when(vaultOperations.opsForSys()).thenReturn(vaultSysOperations);

		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

	@Test
	public void shouldReportSealedService() {

		when(healthResponse.isInitialized()).thenReturn(true);
		when(healthResponse.isSealed()).thenReturn(true);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault sealed");
	}

	@Test
	public void shouldReportUninitializedService() {

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault uninitialized");
	}

	@Test
	public void shouldReportStandbyService() {

		when(healthResponse.isInitialized()).thenReturn(true);
		when(healthResponse.isStandby()).thenReturn(true);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("state", "Vault in standby");
	}

	@Test
	public void exceptionsShouldReportDownStatus() {

		reset(vaultSysOperations);
		when(vaultSysOperations.health()).thenThrow(new IllegalStateException());

		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsKey("error");
	}
}
