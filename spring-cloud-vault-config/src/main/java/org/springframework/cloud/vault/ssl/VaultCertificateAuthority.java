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

import org.springframework.vault.core.VaultPkiOperations;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;

/**
 * Vault PKI {@link CertificateAuthority} implementation.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class VaultCertificateAuthority implements CertificateAuthority {

	private final VaultPkiOperations pki;

	public VaultCertificateAuthority(VaultPkiOperations pki) {
		this.pki = pki;
	}

	@Override
	public CertificateBundle issueCertificate(String bundleName, String roleName, VaultCertificateRequest request) {
		return pki.issueCertificate(roleName, request).getRequiredData();
	}

}
