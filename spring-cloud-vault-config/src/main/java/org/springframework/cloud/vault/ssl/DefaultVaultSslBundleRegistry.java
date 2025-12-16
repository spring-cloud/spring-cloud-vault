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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;
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
	public void register(String roleName, String bundleName, VaultCertificateRequest request) {
		managedSslBundles.put(bundleName,
				new VaultManagedSslBundle(bundleName, roleName, null, SslOptions.NONE, request));
	}

	@Override
	public void register(String bundleName,
			Function<ManagedBundleRoleSpec, ManagedSslBundleSpec> bundleSpecConfigurer) {

		class ManagedSslCertificateSpec implements ManagedBundleRoleSpec, ManagedSslBundleSpec {

			private String roleName;

			private @Nullable String sslProtocol;

			private SslOptions sslOptions = SslOptions.NONE;

			private VaultCertificateRequest request = VaultCertificateRequest.builder().commonName(bundleName).build();

			@Override
			public ManagedSslBundleSpec role(String roleName) {
				Assert.hasText(roleName, "Role name must not be empty");
				this.roleName = roleName;
				return this;
			}

			@Override
			public ManagedSslBundleSpec sslOptions(SslOptions sslOptions) {
				Assert.notNull(sslOptions, "SslOptions must not be null");
				this.sslOptions = sslOptions;
				return this;
			}

			@Override
			public ManagedSslBundleSpec sslProtocol(String sslProtocol) {
				this.sslProtocol = sslProtocol;
				return this;
			}

			@Override
			public ManagedSslBundleSpec request(Consumer<VaultCertificateRequestBuilder> requestConfigurer) {
				Assert.notNull(requestConfigurer, "Request configurer must not be null");
				VaultCertificateRequestBuilder builder = VaultCertificateRequest.builder();
				builder.commonName(bundleName);
				requestConfigurer.accept(builder);
				this.request = builder.build();
				return this;
			}

			@Override
			public ManagedSslBundleSpec request(VaultCertificateRequest certificateRequest) {
				Assert.notNull(certificateRequest, "VaultCertificateRequest must not be null");
				this.request = certificateRequest;
				return this;
			}

		}

		ManagedSslCertificateSpec spec = new ManagedSslCertificateSpec();
		bundleSpecConfigurer.apply(spec);

		managedSslBundles.put(bundleName,
				new VaultManagedSslBundle(bundleName, spec.roleName, spec.sslProtocol, spec.sslOptions, spec.request));
	}

	@Override
	public Collection<VaultManagedSslBundle> getManagedSslBundles() {
		return List.copyOf(managedSslBundles.values());
	}

}
