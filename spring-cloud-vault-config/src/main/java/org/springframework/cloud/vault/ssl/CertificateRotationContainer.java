/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.cloud.vault.ssl;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.format.annotation.DurationFormat.Style;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.support.CertificateBundle;

/**
 * Container to manage certificate rotation for Vault-managed SSL bundles implementing
 * {@link SmartLifecycle}.
 * <p>
 * The container may be restarted after being stopped to resume certificate rotation.
 *
 * @author Mark Paluch
 * @since 5.1
 */
class CertificateRotationContainer implements SmartLifecycle {

	private static final AtomicIntegerFieldUpdater<CertificateRotationContainer> UPDATER = AtomicIntegerFieldUpdater
		.newUpdater(CertificateRotationContainer.class, "status");

	private static final int STATUS_INITIAL = 0;

	private static final int STATUS_STARTED = 1;

	private static final int STATUS_DESTROYED = 2;

	@SuppressWarnings("FieldMayBeFinal") // allow setting via reflection.
	private static Log logger = LogFactory.getLog(CertificateRotationContainer.class);

	private final TaskScheduler taskScheduler;

	private final CertificateAuthority certificateAuthority;

	private Duration expiryThreshold = Duration.ofSeconds(60);

	private final List<CertificateRenewalRequest> renewalRequests = new CopyOnWriteArrayList<>();

	private final Map<CertificateRenewalRequest, CertificateRenewalScheduler> schedulers = new ConcurrentHashMap<>();

	private volatile int status = STATUS_INITIAL;

	CertificateRotationContainer(TaskScheduler taskScheduler, CertificateAuthority certificateAuthority) {
		this.taskScheduler = taskScheduler;
		this.certificateAuthority = certificateAuthority;
	}

	public Duration getExpiryThreshold() {
		return expiryThreshold;
	}

	public void setExpiryThreshold(Duration expiryThreshold) {
		this.expiryThreshold = expiryThreshold;
	}

	@Override
	public void start() {
		Assert.state(this.status != STATUS_DESTROYED, "Container is destroyed and cannot be started");
		Map<CertificateRenewalRequest, CertificateRenewalScheduler> renewals = new HashMap<>(this.schedulers);

		if (UPDATER.compareAndSet(this, STATUS_INITIAL, STATUS_STARTED)) {
			for (Entry<CertificateRenewalRequest, CertificateRenewalScheduler> entry : renewals.entrySet()) {
				start(entry.getKey(), entry.getValue());
			}
		}
	}

	private void start(CertificateRenewalRequest managed, CertificateRenewalScheduler renewalScheduler) {

		ManagedCertificate managedCertificate = managed.issueCertificate();
		logger.debug("Certificate for %s obtained, serial number %s".formatted(managed.managedBundle(),
				managedCertificate.bundle().getSerialNumber()));
		renewalScheduler.scheduleRotation(managedCertificate);
		managed.certificateBundleConsumer().accept(managedCertificate.bundle());
	}

	/**
	 * Stop the {@link SecretLeaseContainer}. Stopping the container will stop lease
	 * renewal, secrets rotation and event publishing. Active leases are not expired.
	 * <p>
	 * Multiple calls are synchronized to stop the container only once.
	 *
	 * @see #start()
	 */
	@Override
	public void stop() {
		if (UPDATER.compareAndSet(this, STATUS_STARTED, STATUS_INITIAL)) {
			for (CertificateRenewalScheduler scheduler : this.schedulers.values()) {
				scheduler.disableScheduleRenewal();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return UPDATER.get(this) == STATUS_STARTED;
	}

	@Override
	public int getPhase() {
		return 200;
	}

	public void addCertificateBundle(VaultManagedSslBundle managedCertificate,
			Consumer<CertificateBundle> certificateBundleConsumer) {

		CertificateRenewalRequest bundle = new CertificateRenewalRequest(managedCertificate, certificateAuthority,
				certificateBundleConsumer);
		this.renewalRequests.add(bundle);

		CertificateRenewalScheduler scheduler = new CertificateRenewalScheduler(this.taskScheduler, bundle,
				this.expiryThreshold);
		this.schedulers.put(bundle, scheduler);

		if (this.status == STATUS_STARTED) {
			start(bundle, scheduler);
		}
	}

	/**
	 * Force certificate rotation.
	 * @param request the managed SSL bundle that is associated with the certificate to
	 * rotate.
	 */
	public void rotate(VaultManagedSslBundle request) {
		for (CertificateRenewalRequest renewalRequest : renewalRequests) {
			if (renewalRequest.managedBundle() == request) {
				CertificateRenewalScheduler scheduler = schedulers.get(renewalRequest);
				if (scheduler != null) {
					scheduler.rotate();
				}
			}
		}
	}

	record CertificateRenewalRequest(VaultManagedSslBundle managedBundle, CertificateAuthority issuer,
			Consumer<CertificateBundle> certificateBundleConsumer) {

		public ManagedCertificate issueCertificate() {
			return new ManagedCertificate(issuer.issueCertificate(managedBundle().name(), managedBundle().roleName(),
					managedBundle().certificateRequest()));
		}
	}

	/**
	 * Renewal scheduler for a managed certificate request.
	 */
	static class CertificateRenewalScheduler {

		private static final AtomicReferenceFieldUpdater<CertificateRenewalScheduler, ManagedCertificate> CURRENT_UPDATER = AtomicReferenceFieldUpdater
			.newUpdater(CertificateRenewalScheduler.class, ManagedCertificate.class, "currentBundleRef");

		private final TaskScheduler taskScheduler;

		private final CertificateRenewalRequest renewalRequest;

		private final VaultManagedSslBundle managedRequest;

		private final Duration expiryThreshold;

		@Nullable volatile ManagedCertificate currentBundleRef;

		final Map<ManagedCertificate, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

		CertificateRenewalScheduler(TaskScheduler taskScheduler, CertificateRenewalRequest renewalRequest,
				Duration expiryThreshold) {
			this.taskScheduler = taskScheduler;
			this.renewalRequest = renewalRequest;
			this.managedRequest = renewalRequest.managedBundle();
			this.expiryThreshold = expiryThreshold;
		}

		void scheduleRotation(ManagedCertificate managedCertificate) {

			long renewalSeconds = getRenewalSeconds(managedCertificate.expiry(), expiryThreshold);

			if (logger.isDebugEnabled()) {
				logger.debug("Scheduling certificate rotation for %s in %s, expiry %s ".formatted(managedRequest,
						DurationFormatterUtils.print(Duration.ofSeconds(renewalSeconds), Style.COMPOSITE),
						managedCertificate.expiry()));
			}

			ManagedCertificate current = CURRENT_UPDATER.get(this);
			CURRENT_UPDATER.set(this, managedCertificate);

			if (current != null) {
				cancelSchedule(current);
			}

			Runnable task = () -> {
				try {
					rotate(managedCertificate);
				}
				catch (Exception e) {
					logger.error(
							"Cannot rotate certificate for %s".formatted(managedRequest, managedRequest.roleName()), e);
				}
			};

			ScheduledFuture<?> scheduledFuture = this.taskScheduler.schedule(task, new OneShotTrigger(renewalSeconds));
			this.schedules.put(managedCertificate, scheduledFuture);
		}

		private ManagedCertificate rotate() {

			ManagedCertificate current = CURRENT_UPDATER.get(this);

			Assert.state(current != null, "No current certificate to rotate");
			return rotate(current);
		}

		private ManagedCertificate rotate(ManagedCertificate managedCertificate) {

			ManagedCertificate current = CURRENT_UPDATER.get(CertificateRenewalScheduler.this);
			if (CURRENT_UPDATER.get(CertificateRenewalScheduler.this) != managedCertificate) {
				return current;
			}

			cancelSchedule(managedCertificate);

			if (logger.isDebugEnabled()) {
				logger.debug("Rotating certificate for %s…".formatted(managedRequest));
			}

			// Renew lease may call scheduleRenewal(…) with a different lease
			// Id to alter set up its own renewal schedule. If it's the old
			// lease, then renewLease() outcome controls the current LeaseId.
			ManagedCertificate renewedCertificate = renewalRequest.issueCertificate();

			if (logger.isDebugEnabled()) {
				logger.debug("Certificate for %s rotated, serial number %s".formatted(managedRequest,
						renewedCertificate.bundle().getSerialNumber()));
			}
			if (CURRENT_UPDATER.compareAndSet(CertificateRenewalScheduler.this, managedCertificate,
					renewedCertificate)) {
				scheduleRotation(renewedCertificate);
				renewalRequest.certificateBundleConsumer().accept(renewedCertificate.bundle());
			}
			else {
				logger.debug("Race condition during certificate rotation of '%s'".formatted(managedRequest));
			}

			return renewedCertificate;
		}

		private void cancelSchedule(ManagedCertificate bundle) {
			ScheduledFuture<?> scheduledFuture = this.schedules.get(bundle);
			if (scheduledFuture != null && !scheduledFuture.isDone() && !scheduledFuture.isCancelled()) {
				scheduledFuture.cancel(false);
			}
		}

		/**
		 * Disables schedule for already scheduled renewals.
		 */
		void disableScheduleRenewal() {
			// capture the previous lease to revoke it
			Set<ManagedCertificate> certificates = new HashSet<>(this.schedules.keySet());
			for (ManagedCertificate certificate : certificates) {
				cancelSchedule(certificate);
				this.schedules.remove(certificate);
			}
		}

		private long getRenewalSeconds(Instant expiration, Duration expiryThreshold) {

			Duration duration = Duration.between(Instant.now(), expiration);
			return Math.max(0, duration.toSeconds() - expiryThreshold.getSeconds());
		}

	}

	record ManagedCertificate(CertificateBundle bundle, X509Certificate certificate, Instant expiry) {

		ManagedCertificate(CertificateBundle certificateBundle) {
			this(certificateBundle, certificateBundle.getX509Certificate());
		}

		ManagedCertificate(CertificateBundle certificateBundle, X509Certificate certificate) {
			this(certificateBundle, certificate, certificate.getNotAfter().toInstant());
		}

	}

	/**
	 * This one-shot trigger creates only one execution time to trigger an execution only
	 * once.
	 */
	static class OneShotTrigger implements Trigger {

		private static final Clock CLOCK = Clock.systemDefaultZone();

		private static final AtomicIntegerFieldUpdater<OneShotTrigger> UPDATER = AtomicIntegerFieldUpdater
			.newUpdater(OneShotTrigger.class, "status");

		private static final int STATUS_ARMED = 0;

		private static final int STATUS_FIRED = 1;

		// see AtomicIntegerFieldUpdater UPDATER
		private volatile int status = 0;

		private final long seconds;

		OneShotTrigger(long seconds) {
			this.seconds = seconds;
		}

		@Override
		public @Nullable Instant nextExecution(TriggerContext triggerContext) {
			return UPDATER.compareAndSet(this, STATUS_ARMED, STATUS_FIRED) ? CLOCK.instant().plusSeconds(this.seconds)
					: null;
		}

	}

}
