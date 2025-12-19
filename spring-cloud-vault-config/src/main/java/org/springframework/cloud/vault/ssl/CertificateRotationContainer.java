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
import java.util.concurrent.ThreadLocalRandom;
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
import org.springframework.util.StringUtils;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.support.Certificate;

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

		ManagedCertificate managedCertificate = managed.getCertificate();
		logger.debug("Certificate for %s obtained, serial number %s".formatted(managed.managedBundle(),
				managedCertificate.getSerialNumber()));
		renewalScheduler.scheduleRotation(managedCertificate);
		managed.certificateConsumer().accept(managedCertificate.certificate());
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
			Consumer<Certificate> certificateConsumer) {

		CertificateRenewalRequest bundle = new CertificateRenewalRequest(managedCertificate, certificateAuthority,
				certificateConsumer);
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

	static Duration getRenewalDelay(Clock clock, Instant expiration, Duration expiryThreshold) {

		Duration expiresIn = Duration.between(clock.instant(), expiration);
		long nextDelay = expiresIn.toSeconds() - expiryThreshold.getSeconds();

		// apply jitter if the expiry is between the expiry threshold and twice the expiry
		// threshold
		if (!expiryThreshold.isZero() && expiresIn.compareTo(expiryThreshold) > 0
				&& expiresIn.minus(expiryThreshold).compareTo(expiryThreshold) > 0) {
			long jitter = Math.min(ThreadLocalRandom.current().nextLong(1, expiryThreshold.toSeconds()),
					expiryThreshold.toSeconds());
			nextDelay += jitter;
		}

		return Duration.ofSeconds(Math.max(0, nextDelay));
	}

	record CertificateRenewalRequest(VaultManagedSslBundle managedBundle, CertificateAuthority issuer,
			Consumer<Certificate> certificateConsumer) {

		public ManagedCertificate getCertificate() {
			if (managedBundle().isIssuerCertificate()) {
				return new ManagedCertificate(
						issuer.getIssuerCertificate(managedBundle().name(), managedBundle().issuer()));
			}
			return new ManagedCertificate(issuer.issueCertificate(managedBundle().name(), managedBundle().roleName(),
					managedBundle().certificateRequest()));
		}
	}

	/**
	 * Renewal scheduler for a managed certificate request.
	 */
	class CertificateRenewalScheduler {

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

			Duration renewalSeconds = getRenewalDelay(taskScheduler.getClock(), managedCertificate.expiry(),
					expiryThreshold);

			if (logger.isDebugEnabled()) {
				logger.debug("Scheduling certificate rotation for %s in %s, expiry %s ".formatted(managedRequest,
						DurationFormatterUtils.print(renewalSeconds, Style.COMPOSITE), managedCertificate.expiry()));
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
			ManagedCertificate renewedCertificate = renewalRequest.getCertificate();

			if (logger.isDebugEnabled()) {
				logger.debug("Certificate for %s rotated, serial number %s".formatted(managedRequest,
						renewedCertificate.getSerialNumber()));
			}
			if (CURRENT_UPDATER.compareAndSet(CertificateRenewalScheduler.this, managedCertificate,
					renewedCertificate)) {
				scheduleRotation(renewedCertificate);
				renewalRequest.certificateConsumer().accept(renewedCertificate.certificate());
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

	}

	record ManagedCertificate(Certificate certificate, X509Certificate x509Certificate, Instant expiry) {

		ManagedCertificate(Certificate certificate) {
			this(certificate, certificate.getX509Certificate());
		}

		ManagedCertificate(Certificate certificate, X509Certificate x509Certificate) {
			this(certificate, x509Certificate, x509Certificate.getNotAfter().toInstant());
		}

		public String getSerialNumber() {

			String serialNumber = certificate.getSerialNumber();
			if (StringUtils.hasText(serialNumber)) {
				return serialNumber;
			}

			byte[] serialBytes = x509Certificate.getSerialNumber().toByteArray();
			while (serialBytes.length != 0 && serialBytes[0] == 0x00) {
				byte[] tmp = new byte[serialBytes.length - 1];
				System.arraycopy(serialBytes, 1, tmp, 0, tmp.length);
				serialBytes = tmp;
			}

			if (serialBytes.length == 0) {
				return "00";
			}

			StringBuilder sb = new StringBuilder();
			for (byte serialByte : serialBytes) {
				if (!sb.isEmpty()) {
					sb.append(":");
				}
				sb.append(String.format("%02x", serialByte));
			}
			return sb.toString();
		}
	}

	/**
	 * This one-shot trigger creates only one execution time to trigger an execution only
	 * once.
	 */
	class OneShotTrigger implements Trigger {

		private static final AtomicIntegerFieldUpdater<OneShotTrigger> UPDATER = AtomicIntegerFieldUpdater
			.newUpdater(OneShotTrigger.class, "status");

		private static final int STATUS_ARMED = 0;

		private static final int STATUS_FIRED = 1;

		// see AtomicIntegerFieldUpdater UPDATER
		private volatile int status = 0;

		private final Duration delay;

		OneShotTrigger(Duration delay) {
			this.delay = delay;
		}

		@Override
		public @Nullable Instant nextExecution(TriggerContext triggerContext) {
			return UPDATER.compareAndSet(this, STATUS_ARMED, STATUS_FIRED)
					? taskScheduler.getClock().instant().plus(this.delay) : null;
		}

	}

}
