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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.client.VaultException;
import org.springframework.vault.client.VaultResponseEntity;
import org.springframework.vault.core.VaultOperations;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link VaultPropertySource} that renews a {@link Lease} associated with
 * {@link Secrets}.
 *
 * <p>
 * {@link Lease} is scheduled right before its expiry. Expiry threshold can be set by
 * calling {@link #setExpiryThresholdSeconds(int)}. Leases that reached their maximum
 * lifetime are not re-read from Vault.
 *
 * @author Mark Paluch
 */
@Slf4j
class LeasingVaultPropertySource extends VaultPropertySource implements DisposableBean {

	private final LeaseRenewalScheduler leaseRenewal;

	private int minRenewalSeconds = 10;

	private int expiryThresholdSeconds = 60;

	private volatile Lease lease;

	/**
	 * Creates a new {@link VaultPropertySource}.
	 *
	 * @param operations must not be {@literal null}.
	 * @param failFast fail if properties could not be read because of access errors.
	 * @param secretBackendMetadata must not be {@literal null}.
	 * @param taskScheduler must not be {@literal null}.
	 */
	public LeasingVaultPropertySource(VaultConfigOperations operations, boolean failFast,
			SecretBackendMetadata secretBackendMetadata,
			TaskScheduler taskScheduler) {

		super(operations, failFast, secretBackendMetadata);

		Assert.notNull(taskScheduler, "TaskScheduler must not be null");

		leaseRenewal = new LeaseRenewalScheduler(taskScheduler);
	}

	/**
	 * Set the expiry threshold. {@link Lease} is renewed the given seconds before it
	 * expires.
	 *
	 * @param expiryThresholdSeconds number of seconds before {@link Lease} expiry.
	 */
	public void setExpiryThresholdSeconds(int expiryThresholdSeconds) {
		this.expiryThresholdSeconds = expiryThresholdSeconds;
	}

	/**
	 * Sets the amount of seconds that is at least required before renewing a lease.
	 * {@code minRenewalSeconds} prevents renewals to happen too often.
	 *
	 * @param minRenewalSeconds number of seconds that is at least required before
	 * renewing a {@link Lease}.
	 */
	public void setMinRenewalSeconds(int minRenewalSeconds) {
		this.minRenewalSeconds = minRenewalSeconds;
	}

	@Override
	public void init() {

		super.init();

		Secrets secrets = getSecrets();

		this.lease = getLease(secrets);

		potentiallyScheduleLeaseRenewal(this.lease);
	}

	/**
	 * Shutdown this {@link LeasingVaultPropertySource}
	 */
	public void destroy() {

		if (this.lease != null) {
			try {
				leaseRenewal.disableScheduleRenewal();
				doRevokeLease(this.lease);
			}
			finally {
				this.lease = null;
			}
		}
	}

	private Lease getLease(Secrets secrets) {

		if (secrets == null || !StringUtils.hasText(secrets.getLeaseId())) {
			return null;
		}

		return Lease.of(secrets.getLeaseId(), secrets.getLeaseDuration(),
				secrets.isRenewable());
	}

	private void potentiallyScheduleLeaseRenewal(Lease lease) {

		if (leaseRenewal.isLeaseRenewable(lease)) {

			if (log.isDebugEnabled()) {
				log.debug(String.format("Lease %s qualified for renewal",
						lease.getLeaseId()));
			}

			leaseRenewal.scheduleRenewal(new RenewLease() {
				@Override
				public Lease renewLease(Lease lease) {

					Lease newLease = doRenewLease(lease);
					LeasingVaultPropertySource.this.lease = newLease;
					potentiallyScheduleLeaseRenewal(newLease);

					return newLease;
				}
			}, lease, minRenewalSeconds, expiryThresholdSeconds);
		}
	}

	/**
	 * Renews a {@link Lease}.
	 *
	 * @param lease the lease
	 * @return the new lease.
	 */
	private Lease doRenewLease(final Lease lease) {

		VaultResponseEntity<Map<String, Object>> entity = getSource().getVaultOperations()
				.doWithVault(
						new VaultOperations.SessionCallback<VaultResponseEntity<Map<String, Object>>>() {
							@Override
							public VaultResponseEntity<Map<String, Object>> doWithVault(
									VaultOperations.VaultSession session) {

								return session.putForEntity(
										String.format("sys/renew/%s", lease.getLeaseId()),
										null, Map.class);
							}

						});

		if (entity.isSuccessful() && entity.hasBody()) {

			Map<String, Object> body = entity.getBody();
			String leaseId = (String) body.get("lease_id");
			Number leaseDuration = (Number) body.get("lease_duration");
			boolean renewable = (Boolean) body.get("renewable");

			if (!StringUtils.hasText(leaseId)) {
				return null;
			}

			return Lease.of(leaseId,
					leaseDuration != null ? leaseDuration.longValue() : 0, renewable);
		}

		throw new VaultException(
				String.format("Cannot renew lease: %s", buildExceptionMessage(entity)));
	}

	/**
	 * Revokes the {@link Lease}.
	 *
	 * @param lease the lease.
	 */
	private void doRevokeLease(final Lease lease) {

		VaultResponseEntity<Map<String, Object>> entity = getSource().getVaultOperations()
				.doWithVault(
						new VaultOperations.SessionCallback<VaultResponseEntity<Map<String, Object>>>() {
							@Override
							public VaultResponseEntity<Map<String, Object>> doWithVault(
									VaultOperations.VaultSession session) {

								return session.putForEntity(String.format("sys/revoke/%s",
										lease.getLeaseId()), null, Map.class);
							}

						});

		if (entity.isSuccessful()) {
			return;
		}

		throw new VaultException(
				String.format("Cannot revoke lease: %s", buildExceptionMessage(entity)));
	}

	private static String buildExceptionMessage(VaultResponseEntity<?> response) {

		if (StringUtils.hasText(response.getMessage())) {
			return String.format("Status %s URI %s: %s", response.getStatusCode(),
					response.getUri(), response.getMessage());
		}

		return String.format("Status %s URI %s", response.getStatusCode(),
				response.getUri());
	}

	/**
	 * Abstracts scheduled lease renewal. A {@link LeaseRenewalScheduler} can be accessed
	 * concurrently to schedule lease renewal. Each renewal run checks if the previously
	 * attached {@link Lease} is still relevant to update. If any other process scheduled
	 * a newer {@link Lease} for renewal, the previously registered renewal task will skip
	 * renewal.
	 */
	private static class LeaseRenewalScheduler {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		private final TaskScheduler taskScheduler;

		private final AtomicReference<Lease> currentLease = new AtomicReference<>();

		private final Map<Lease, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

		/**
		 *
		 * @param taskScheduler must not be {@literal null}.
		 */
		LeaseRenewalScheduler(TaskScheduler taskScheduler) {
			this.taskScheduler = taskScheduler;
		}

		/**
		 * Schedule {@link Lease} renewal. Previously registered renewal tasks are
		 * canceled to prevent renewal of stale {@link Lease}s.
		 * @param renewLease strategy to renew a {@link Lease}.
		 * @param lease the current {@link Lease}.
		 * @param minRenewalSeconds minimum number of seconds before renewing a
		 * {@link Lease}. This is to prevent too many renewals in a very short timeframe.
		 * @param expiryThresholdSeconds number of seconds to renew before {@link Lease}.
		 * expires.
		 */
		void scheduleRenewal(final RenewLease renewLease, final Lease lease,
				final int minRenewalSeconds, final int expiryThresholdSeconds) {

			logger.debug("Scheduling renewal for lease {}, lease duration {}",
					lease.getLeaseId(), lease.getLeaseDuration());

			Lease currentLease = this.currentLease.get();
			this.currentLease.set(lease);

			if (currentLease != null) {
				cancelSchedule(currentLease);
			}

			ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(new Runnable() {

				@Override
				public void run() {

					try {
						schedules.remove(lease);

						if (LeaseRenewalScheduler.this.currentLease.get() != lease) {
							logger.debug("Current lease has changed. Skipping renewal");
							return;
						}

						logger.debug("Renewing lease {}", lease.getLeaseId());
						LeaseRenewalScheduler.this.currentLease.compareAndSet(lease,
								renewLease.renewLease(lease));
					}
					catch (Exception e) {
						logger.error("Cannot renew lease {}", lease.getLeaseId(), e);
					}
				}
			}, new OneShotTrigger(
					getRenewalSeconds(lease, minRenewalSeconds, expiryThresholdSeconds)));

			schedules.put(lease, scheduledFuture);
		}

		private void cancelSchedule(Lease lease) {

			ScheduledFuture<?> scheduledFuture = schedules.get(lease);
			if (scheduledFuture != null) {
				logger.debug("Canceling previously registered schedule for lease {}",
						lease.getLeaseId());
				scheduledFuture.cancel(false);
			}
		}

		/**
		 * Disables schedule for already scheduled renewals.
		 */
		public void disableScheduleRenewal() {

			currentLease.set(null);
			Set<Lease> leases = new HashSet<>(schedules.keySet());

			for (Lease lease : leases) {
				cancelSchedule(lease);
				schedules.remove(lease);
			}
		}

		private long getRenewalSeconds(Lease lease, int minRenewalSeconds,
				int expiryThresholdSeconds) {
			return Math.max(minRenewalSeconds,
					lease.getLeaseDuration() - expiryThresholdSeconds);
		}

		private boolean isLeaseRenewable(Lease lease) {
			return lease != null && lease.isRenewable();
		}

	}

	/**
	 * This one-shot trigger creates only one execution time to trigger an execution only
	 * once.
	 */
	private static class OneShotTrigger implements Trigger {

		private final AtomicBoolean fired = new AtomicBoolean();

		private final long seconds;

		OneShotTrigger(long seconds) {
			this.seconds = seconds;
		}

		@Override
		public Date nextExecutionTime(TriggerContext triggerContext) {

			if (fired.compareAndSet(false, true)) {
				return new Date(
						System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds));
			}

			return null;
		}
	}

	/**
	 * Strategy interface to renew a {@link Lease}.
	 */
	private interface RenewLease {

		/**
		 * Renew a lease.
		 *
		 * @param lease must not be {@literal null}.
		 * @return the new lease
		 * @throws VaultException if lease renewal runs into problems
		 */
		Lease renewLease(Lease lease) throws VaultException;
	}
}
