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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.x500.X500Principal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.core.VaultPkiOperations;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;

/**
 * Integration test for {@link CertificateRotationContainer}.
 *
 * @author Mark Paluch
 */
class CertificateRotationContainerIntegrationTests extends PkiIntegrationTestSupport {

	ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

	VaultManagedSslBundle request = new VaultManagedSslBundle("www.example.com", "testrole",
			VaultCertificateRequest.builder().commonName("www.example.com").ttl(Duration.ofSeconds(80)).build());

	CertificateRotationContainer container;

	@BeforeEach
	@Override
	public void setUp() {
		super.setUp();
		taskScheduler.afterPropertiesSet();
		taskScheduler.start();

		VaultPkiOperations pki = template.opsForPki();
		container = new CertificateRotationContainer(taskScheduler, (certificateBundle, roleName, request) -> {
			return pki.issueCertificate(roleName, request).getRequiredData();
		});
	}

	@AfterEach
	void tearDown() {
		taskScheduler.stop();
	}

	@Test
	void startedContainerShouldRequestCertificate() {

		container.start();

		AtomicReference<CertificateBundle> bundleRef = new AtomicReference<>();

		container.addCertificateBundle(request, bundleRef::set);

		container.stop();

		assertThat(bundleRef.get()).isNotNull();
		assertThat(bundleRef).hasValueSatisfying(actual -> {
			assertThat(actual.getX509Certificate().getSubjectX500Principal().getName(X500Principal.CANONICAL))
				.contains("cn=www.example.com");
		});
	}

	@Test
	void shouldRequestCertificate() {

		AtomicReference<CertificateBundle> bundleRef = new AtomicReference<>();
		container.addCertificateBundle(request, bundleRef::set);

		assertThat(bundleRef).hasValue(null);

		container.start();
		container.stop();

		assertThat(bundleRef.get()).isNotNull();
		assertThat(bundleRef).hasValueSatisfying(actual -> {
			assertThat(actual.getX509Certificate().getSubjectX500Principal().getName(X500Principal.CANONICAL))
				.contains("cn=www.example.com");
		});
	}

	@Test
	void shouldRotateCertificate() {

		container.start();
		AtomicReference<CertificateBundle> bundleRef = new AtomicReference<>();
		container.addCertificateBundle(request, bundleRef::set);

		assertThat(bundleRef.get()).isNotNull();
		CertificateBundle initial = bundleRef.get();
		container.rotate(request);
		CertificateBundle rotated = bundleRef.get();
		container.stop();

		assertThat(initial).isNotEqualTo(rotated);
		assertThat(initial.getX509Certificate().getSerialNumber())
			.isNotEqualTo(rotated.getX509Certificate().getSerialNumber());
	}

}
