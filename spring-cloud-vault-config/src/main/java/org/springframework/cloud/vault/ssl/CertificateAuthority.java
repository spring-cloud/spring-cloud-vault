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

import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;

/**
 * Interface representing a Certificate Authority to issue certificates.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@FunctionalInterface
public interface CertificateAuthority {

	/**
	 * Issue (or re-issue) a certificate for the given {@code bundleName} using the role
	 * name and {@link VaultCertificateRequest}.
	 * @param bundleName name of the certificate bundle.
	 * @param roleName Vault role name
	 * @param request the {@link VaultCertificateRequest}.
	 * @return the issued (or re-issued) {@link CertificateBundle}.
	 */
	CertificateBundle issueCertificate(String bundleName, String roleName, VaultCertificateRequest request);

}
