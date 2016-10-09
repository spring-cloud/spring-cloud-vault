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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.vault.client.VaultResponseEntity;
import org.springframework.vault.core.VaultOperations;

/**
 * Unit tests for {@link LeasingVaultPropertySource}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class LeasingVaultPropertySourceUnitTests {

	@Mock
	private VaultConfigTemplate configOperations;

	@Mock
	private VaultOperations vaultOperations;

	@Mock
	private SecretBackendMetadata secretBackendMetadata;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ScheduledFuture scheduledFuture;

	private LeasingVaultPropertySource propertySource;

	@Before
	public void before() throws Exception {

		when(secretBackendMetadata.getName()).thenReturn("test");
		when(configOperations.getVaultOperations()).thenReturn(vaultOperations);

		propertySource = new LeasingVaultPropertySource(configOperations,
				false, secretBackendMetadata, taskScheduler);
	}

	@Test
	public void shouldWorkIfSecretsNotFound() {

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).isEmpty();
	}

	@Test
	public void shouldAcceptSecretsWithoutLease() {

		Secrets secrets = new Secrets();
		secrets.setData(Collections.singletonMap("key", "value"));

		when(configOperations.read(secretBackendMetadata)).thenReturn(secrets);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).contains("key");
		verifyZeroInteractions(taskScheduler);
	}

	@Test
	public void shouldAcceptSecretsWithStaticLease() {

		Secrets secrets = new Secrets();
		secrets.setLeaseId("lease");
		secrets.setRenewable(false);
		secrets.setData(Collections.singletonMap("key", "value"));

		when(configOperations.read(secretBackendMetadata)).thenReturn(secrets);

		propertySource.init();

		verifyZeroInteractions(taskScheduler);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldAcceptSecretsWithRenewableLease() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
				.thenReturn(scheduledFuture);
		when(configOperations.read(secretBackendMetadata)).thenReturn(createSecrets());

		propertySource.init();

		verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldRenewLease() {

		prepareRenewal();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		captor.getValue().run();
		verifyZeroInteractions(scheduledFuture);
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void scheduleRenewalShouldApplyExpiryThreshold() {

		prepareRenewal();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler).schedule(any(Runnable.class), captor.capture());

		Date nextExecutionTime = captor.getValue().nextExecutionTime(null);
		assertThat(nextExecutionTime).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(35)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(41)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subsequentScheduleRenewalShouldApplyExpiryThreshold() {

		prepareRenewal();

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(runnableCaptor.capture(), any(Trigger.class));

		runnableCaptor.getValue().run();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler, times(2)).schedule(any(Runnable.class), captor.capture());

		assertThat(captor.getAllValues().get(0).nextExecutionTime(null)).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(35)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(41)));

		assertThat(captor.getAllValues().get(1).nextExecutionTime(null)).isBetween(
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(9)),
				new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(11)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void scheduleRenewalShouldTriggerOnlyOnce() {

		prepareRenewal();

		ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
		verify(taskScheduler).schedule(any(Runnable.class), captor.capture());

		Trigger trigger = captor.getValue();

		assertThat(trigger.nextExecutionTime(null)).isNotNull();
		assertThat(trigger.nextExecutionTime(null)).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void subsequentInitShouldCancelExistingSchedule() {

		prepareRenewal();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		propertySource.init();

		verify(scheduledFuture).cancel(false);
		verify(taskScheduler, times(2)).schedule(captor.capture(), any(Trigger.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void canceledRenewalShouldSkipRenewal() {

		prepareRenewal();

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(captor.capture(), any(Trigger.class));

		propertySource.init();
		verify(scheduledFuture).cancel(false);

		captor.getValue().run();

		verifyZeroInteractions(vaultOperations);
	}

	@Test
	public void shouldDisableRenewalOnDisposal() {

		prepareRenewal();

		propertySource.destroy();

		verify(vaultOperations).doWithVault(any(VaultOperations.SessionCallback.class));
		verify(scheduledFuture).cancel(false);
	}

	private void prepareRenewal() {

		when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
				.thenReturn(scheduledFuture);
		when(configOperations.read(secretBackendMetadata)).thenReturn(createSecrets());
		propertySource.init();
		when(vaultOperations.doWithVault(any(VaultOperations.SessionCallback.class)))
				.thenReturn(getResponseEntity("new_lease", true, 70, HttpStatus.OK));
	}

	private VaultResponseEntity<Map<String, Object>> getResponseEntity(String leaseId,
			Boolean renewable, Integer leaseDuration, HttpStatus httpStatus) {

		Map<String, Object> body = new HashMap<>();
		body.put("lease_id", leaseId);
		body.put("renewable", renewable);
		body.put("lease_duration", leaseDuration);

		return getEntity(body, httpStatus);
	}

	private VaultResponseEntity<Map<String, Object>> getEntity(Map<String, Object> body,
			HttpStatus status) {

		return new VaultResponseEntity<Map<String, Object>>(body, status, null, null) {
		};
	}

	private Secrets createSecrets() {

		Secrets secrets = new Secrets();

		secrets.setLeaseId("lease");
		secrets.setRenewable(true);
		secrets.setLeaseDuration(100);
		secrets.setData(Collections.singletonMap("key", "value"));

		return secrets;
	}
}