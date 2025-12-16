/*
 * Copyright 2026-present the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;
import org.springframework.vault.core.certificate.domain.RequestedCertificate;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateRequest.VaultCertificateRequestBuilder;

/**
 * Default {@link ListableVaultSslBundleRegistry} implementation.
 *
 * @author Mark Paluch
 * @since 5.1
 */
class DefaultVaultSslBundleRegistry implements ListableVaultSslBundleRegistry {

	private final Map<String, VaultManagedSslBundle> managedSslBundles = new ConcurrentHashMap<>();

	@Override
	public void register(String bundleName, String roleName, VaultCertificateRequest request) {
		this.managedSslBundles.put(bundleName,
				new VaultManagedSslBundle(RequestedCertificate.issue(bundleName, roleName, request)));
	}

	@Override
	public void register(String bundleName, Function<ManagedSslBundle, ManagedSslBundleSpec<?>> bundleSpecConfigurer) {

		class ManagedIssuerCertificateSpec implements VaultSslBundleRegistry.ManagedIssuerCertificate {

			private final String issuerName;

			private @Nullable String sslProtocol;

			private SslOptions sslOptions = SslOptions.NONE;

			ManagedIssuerCertificateSpec(String issuerName) {
				this.issuerName = issuerName;
			}

			@Override
			public ManagedIssuerCertificateSpec sslOptions(SslOptions sslOptions) {
				Assert.notNull(sslOptions, "SslOptions must not be null");
				this.sslOptions = sslOptions;
				return this;
			}

			@Override
			public ManagedIssuerCertificateSpec sslProtocol(String sslProtocol) {
				this.sslProtocol = sslProtocol;
				return this;
			}

		}

		class ManagedCertificateRequestSpec implements ManagedCertificateRequest {

			private final String roleName;

			private @Nullable String sslProtocol;

			private SslOptions sslOptions = SslOptions.NONE;

			private VaultCertificateRequest request;

			ManagedCertificateRequestSpec(String roleName) {
				this.roleName = roleName;
				this.request = VaultCertificateRequest.builder().commonName(bundleName).build();
			}

			@Override
			public ManagedCertificateRequestSpec sslOptions(SslOptions sslOptions) {
				Assert.notNull(sslOptions, "SslOptions must not be null");
				this.sslOptions = sslOptions;
				return this;
			}

			@Override
			public ManagedCertificateRequestSpec sslProtocol(String sslProtocol) {
				this.sslProtocol = sslProtocol;
				return this;
			}

			@Override
			public ManagedCertificateRequestSpec request(Consumer<VaultCertificateRequestBuilder> requestConfigurer) {
				Assert.notNull(requestConfigurer, "Request configurer must not be null");
				VaultCertificateRequestBuilder builder = VaultCertificateRequest.builder();
				builder.commonName(bundleName);
				requestConfigurer.accept(builder);
				this.request = builder.build();
				return this;
			}

			@Override
			public ManagedCertificateRequestSpec request(VaultCertificateRequest certificateRequest) {
				Assert.notNull(certificateRequest, "VaultCertificateRequest must not be null");
				this.request = certificateRequest;
				return this;
			}

		}

		ManagedSslBundle managedSslBundle = new ManagedSslBundle() {
			@Override
			public ManagedIssuerCertificateSpec trustAnchor(String issuer) {
				return new ManagedIssuerCertificateSpec(issuer);
			}

			@Override
			public ManagedCertificateRequestSpec issueCertificate(String roleName) {
				return new ManagedCertificateRequestSpec(roleName);
			}
		};

		ManagedSslBundleSpec<?> spec = bundleSpecConfigurer.apply(managedSslBundle);

		if (spec instanceof ManagedCertificateRequestSpec mcr) {
			this.managedSslBundles.put(bundleName,
					new VaultManagedSslBundle(RequestedCertificate.issue(bundleName, mcr.roleName, mcr.request),
							mcr.sslProtocol, mcr.sslOptions));
		}
		else if (spec instanceof ManagedIssuerCertificateSpec mic) {
			this.managedSslBundles.put(bundleName, new VaultManagedSslBundle(
					RequestedCertificate.trustAnchor(bundleName, mic.issuerName), mic.sslProtocol, mic.sslOptions));
		}
	}

	@Override
	public Collection<VaultManagedSslBundle> getManagedSslBundles() {
		return List.copyOf(this.managedSslBundles.values());
	}

}
