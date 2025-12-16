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
import org.springframework.vault.core.certificate.domain.RequestedCertificate;
import org.springframework.vault.core.certificate.domain.RequestedCertificateBundle;
import org.springframework.vault.core.certificate.domain.RequestedTrustAnchor;

/**
 * Value object to capture a request for a Vault-managed SSL bundle.
 *
 * @author Mark Paluch
 * @since 5.1
 */
record VaultManagedSslBundle(RequestedCertificate requestedCertificate, @Nullable String sslProtocol,
		SslOptions sslOptions) {

	VaultManagedSslBundle(RequestedCertificate requestedCertificate) {
		this(requestedCertificate, null, SslOptions.NONE);
	}

	@Override
	public String toString() {

		if (this.requestedCertificate instanceof RequestedTrustAnchor trustAnchor) {
			return "Vault-managed Trust Anchor SslBundle '%s' (issuer='%s')".formatted(getName(),
					trustAnchor.getIssuer());
		}

		if (this.requestedCertificate instanceof RequestedCertificateBundle bundle) {
			return "Vault-managed SslBundle '%s' (cn='%s', role='%s')".formatted(getName(),
					bundle.getRequest().getCommonName(), bundle.getRole());
		}
		return "Vault-managed SslBundle '%s'".formatted(getName());
	}

	String getName() {
		return this.requestedCertificate.getName();
	}
}
