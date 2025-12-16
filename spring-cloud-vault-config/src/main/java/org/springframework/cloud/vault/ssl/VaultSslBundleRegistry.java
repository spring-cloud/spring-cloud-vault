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

import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateRequest.VaultCertificateRequestBuilder;

/**
 * Interface to register Vault-managed {@link org.springframework.boot.ssl.SslBundle
 * SslBundles}.
 * <p>
 * Vault-managed SSL bundles obtain their certificates from Vault using the
 * {@link org.springframework.vault.core.VaultPkiOperations PKI backend} for certificate
 * renewal and rotation.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public interface VaultSslBundleRegistry {

	/**
	 * Register a Vault-managed SSL bundle given {@code roleName}, {@code bundleName} and
	 * {@link VaultCertificateRequest}.
	 * @param bundleName name of the SSL bundle under which the bundle will be
	 * {@link org.springframework.boot.ssl.SslBundleRegistry#registerBundle(String, SslBundle)
	 * registered}.
	 * @param roleName name of the Vault role that applies to this
	 * {@link VaultCertificateRequest certificate request}.
	 * @param request the {@link VaultCertificateRequest} defining all request parameters.
	 */
	void register(String bundleName, String roleName, VaultCertificateRequest request);

	/**
	 * Register a Vault-managed SSL bundle given {@code bundleName} and a spec
	 * {@code bundleSpecConfigurer}.
	 * @param bundleName name of the bundle. When using a managed certificate, the bundle
	 * name also serves as the common name for the certificate request unless overridden
	 * in the {@link VaultCertificateRequest}.
	 * @param bundleSpecConfigurer a function receiving a {@link ManagedSslBundle} to
	 * configure the bundle.
	 */
	void register(String bundleName, Function<ManagedSslBundle, ManagedSslBundleSpec<?>> bundleSpecConfigurer);

	/**
	 * Interface to configure the issuer or role name for a managed SSL bundle and
	 * continue with SSL options and the certificate request configuration.
	 */
	interface ManagedSslBundle {

		/**
		 * Configure a managed trust anchor SSL bundle by obtaining the CA certificate for
		 * {@code issuer}.
		 * @param issuer name of the issuer.
		 * @return the {@link ManagedIssuerCertificate} to continue configuration.
		 */
		ManagedIssuerCertificate trustAnchor(String issuer);

		/**
		 * Configure a managed SSL bundle by issuing a certificate using {@code roleName}.
		 * @param roleName name of the Vault role.
		 * @return the {@link ManagedSslBundleSpec} to continue configuration.
		 */
		ManagedCertificateRequest issueCertificate(String roleName);

	}

	/**
	 * Interface to configure SSL options and protocol for a managed SSL bundle.
	 */
	interface ManagedSslBundleSpec<M extends ManagedSslBundleSpec<M>> {

		/**
		 * Configure {@link SslOptions}.
		 * @param sslOptions the SSL options, defaults to {@link SslOptions#NONE}.
		 * @return this builder.
		 */
		M sslOptions(SslOptions sslOptions);

		/**
		 * SSL protocol name used for {@link SSLContext#getInstance(String)}.
		 * @param sslProtocol name of the SSL protocol.
		 * @return this builder.
		 */
		M sslProtocol(String sslProtocol);

	}

	/**
	 * Interface to configure SSL options and protocol for a managed SSL bundle for issuer
	 * certificates.
	 */
	interface ManagedIssuerCertificate extends ManagedSslBundleSpec<ManagedIssuerCertificate> {

	}

	/**
	 * Interface to configure SSL options, protocol and the certificate request for a
	 * managed SSL bundle.
	 */
	interface ManagedCertificateRequest extends ManagedSslBundleSpec<ManagedCertificateRequest> {

		/**
		 * Configure the {@link VaultCertificateRequest} using the given
		 * {@code requestConfigurer}.
		 * @param requestConfigurer the request configurer callback.
		 * @return this builder.
		 */
		ManagedCertificateRequest request(Consumer<VaultCertificateRequestBuilder> requestConfigurer);

		/**
		 * Configure the {@link VaultCertificateRequest}.
		 * @param certificateRequest the certificate request object.
		 * @return this builder.
		 */
		ManagedCertificateRequest request(VaultCertificateRequest certificateRequest);

	}

}
