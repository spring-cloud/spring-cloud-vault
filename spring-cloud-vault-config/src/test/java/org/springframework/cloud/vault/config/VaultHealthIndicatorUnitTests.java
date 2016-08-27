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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.vault.VaultHealthResponse;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class VaultHealthIndicatorUnitTests {

	@InjectMocks
	VaultHealthIndicator healthIndicator = new VaultHealthIndicator();

	@Mock
	VaultTemplate vaultTemplate;

	@Test
	public void shouldReportHealthyService() throws Exception {

		VaultHealthResponse healthResponse = new VaultHealthResponse();
		healthResponse.setInitialized(true);

		when(vaultTemplate.health()).thenReturn(healthResponse);

		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

	@Test
	public void shouldReportSealedService() throws Exception {

		VaultHealthResponse healthResponse = new VaultHealthResponse();
		healthResponse.setInitialized(true);
		healthResponse.setSealed(true);

		when(vaultTemplate.health()).thenReturn(healthResponse);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault sealed");
	}

	@Test
	public void shouldReportUninitializedService() throws Exception {

		VaultHealthResponse healthResponse = new VaultHealthResponse();

		when(vaultTemplate.health()).thenReturn(healthResponse);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("state", "Vault uninitialized");
	}

	@Test
	public void shouldReportStandbyService() throws Exception {

		VaultHealthResponse healthResponse = new VaultHealthResponse();
		healthResponse.setInitialized(true);
		healthResponse.setStandby(true);

		when(vaultTemplate.health()).thenReturn(healthResponse);

		Health health = healthIndicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertThat(health.getDetails()).containsEntry("state", "Vault in standby");
	}

	@Test
	public void exceptionsShouldReportDownStatus() throws Exception {

		when(vaultTemplate.health()).thenThrow(new IllegalStateException());

		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).isEmpty();
	}
}