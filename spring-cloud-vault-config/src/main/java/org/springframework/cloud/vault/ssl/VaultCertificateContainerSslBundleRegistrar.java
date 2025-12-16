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

import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.vault.core.certificate.CertificateContainer;
import org.springframework.vault.core.certificate.ManagedCertificate;
import org.springframework.vault.support.Certificate;
import org.springframework.vault.support.CertificateBundle;

/**
 * {@link SslBundleRegistrar} that uses Vault's PKI secret backend to issue certificates
 * and obtain issuer certificates for managed SSL bundles with automatic rotation.
 *
 * @author Mark Paluch
 * @since 5.1
 */
class VaultCertificateContainerSslBundleRegistrar implements SslBundleRegistrar {

	private final CertificateContainer certificateContainer;

	private final ListableVaultSslBundleRegistry vaultSslBundleRegistry;

	VaultCertificateContainerSslBundleRegistrar(CertificateContainer certificateContainer,
			ListableVaultSslBundleRegistry vaultSslBundleRegistry) {
		this.certificateContainer = certificateContainer;
		this.vaultSslBundleRegistry = vaultSslBundleRegistry;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {

		for (VaultManagedSslBundle managedBundle : this.vaultSslBundleRegistry.getManagedSslBundles()) {

			AtomicBoolean initial = new AtomicBoolean(true);
			ManagedCertificate managedCertificate = ManagedCertificate.from(managedBundle.requestedCertificate(),
					certificate -> {
						SslBundle sslBundle = toSslBundle(certificate, managedBundle);
						if (initial.compareAndSet(true, false)) {
							registry.registerBundle(managedBundle.getName(), sslBundle);
						}
						else {
							registry.updateBundle(managedBundle.getName(), sslBundle);
						}
					});

			managedCertificate.registerCertificate(this.certificateContainer);
		}
	}

	private SslBundle toSslBundle(Certificate certificate, VaultManagedSslBundle request) {
		if (certificate instanceof CertificateBundle bundle) {
			String random = UUID.randomUUID().toString();
			SslBundleKey key = SslBundleKey.of(random, request.getName());
			KeyStore keyStore = bundle.createKeyStore(request.getName(), true, random);
			KeyStore trustStore = bundle.createTrustStore(true);
			SslStoreBundle storeBundle = SslStoreBundle.of(keyStore, null, trustStore);
			return SslBundle.of(storeBundle, key, request.sslOptions(), request.sslProtocol());
		}

		KeyStore trustStore = certificate.createTrustStore(true);
		SslStoreBundle storeBundle = SslStoreBundle.of(null, null, trustStore);
		return SslBundle.of(storeBundle, null, request.sslOptions(), request.sslProtocol());
	}

}
