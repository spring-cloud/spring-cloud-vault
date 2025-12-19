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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslOptions;
import org.springframework.vault.support.VaultCertificateRequest;

/**
 * Value object to capture a request for a Vault-managed SSL bundle.
 *
 * @author Mark Paluch
 * @since 5.1
 */
record VaultManagedSslBundle(String name, @Nullable String roleName, @Nullable String sslProtocol,
		SslOptions sslOptions, @Nullable String issuer, @Nullable VaultCertificateRequest certificateRequest) {

	VaultManagedSslBundle(String name, String roleName, VaultCertificateRequest certificateRequest) {
		this(name, roleName, null, SslOptions.NONE, null, certificateRequest);
	}

	VaultManagedSslBundle(String name, String issuer, @Nullable String sslProtocol, SslOptions sslOptions) {
		this(name, null, sslProtocol, sslOptions, issuer, null);
	}

	VaultManagedSslBundle(String name, String roleName, @Nullable String sslProtocol, SslOptions sslOptions,
			VaultCertificateRequest certificateRequest) {
		this(name, roleName, sslProtocol, sslOptions, null, certificateRequest);
	}

	VaultManagedSslBundle(String name, String issuer) {
		this(name, null, null, SslOptions.NONE, issuer, null);
	}

	public boolean isIssuerCertificate() {
		return this.issuer != null;
	}

	public boolean isManagedCertificate() {
		return this.certificateRequest != null;
	}

	@Override
	public String toString() {

		if (this.certificateRequest == null) {
			return "Vault-managed SslBundle '%s' (issuer='%s')".formatted(name, issuer);
		}
		return "Vault-managed SslBundle '%s' (cn='%s', role='%s')".formatted(name, certificateRequest.getCommonName(),
				roleName);
	}
}
