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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.ssl.SslBundleProperties.Options;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;

/**
 * Configuration properties for Vault-managed SSL bundles using X.509 certificates.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@ConfigurationProperties("spring.cloud.vault.ssl")
public class VaultSslBundlesProperties {

	private Lifecycle lifecycle = new Lifecycle();

	/**
	 * Map of named Vault-managed SSL bundles to issue private keys along with
	 * certificates.
	 */
	private final Map<String, VaultSslBundle> bundle = new LinkedHashMap<>();

	/**
	 * Map of named Vault-managed trust bundles that contain CA issuer certificates for
	 * trust anchor usage.
	 */
	private final Map<String, VaultIssuerCertificateBundle> trustBundle = new LinkedHashMap<>();

	public Lifecycle getLifecycle() {
		return this.lifecycle;
	}

	public void setLifecycle(Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}

	public Map<String, VaultSslBundle> getBundle() {
		return this.bundle;
	}

	public Map<String, VaultIssuerCertificateBundle> getTrustBundle() {
		return this.trustBundle;
	}

	/**
	 * Properties for a Vault-managed SSL bundle.
	 */
	public static class Lifecycle {

		/**
		 * Expiry threshold to trigger certificate renewal before actual expiry.
		 */
		private Duration expiryThreshold = Duration.ofMinutes(1);

		/**
		 * Mount path of the PKI backend in Vault.
		 */
		private String pkiMount = "pki";

		/**
		 * Configuration for storing the certificate in a versioned Vault Key/Value secret
		 * store. Useful to avoid repetitive certificate creation across application
		 * restarts.
		 */
		private @Nullable CertificateStore store;

		public Duration getExpiryThreshold() {
			return this.expiryThreshold;
		}

		public void setExpiryThreshold(Duration expiryThreshold) {
			this.expiryThreshold = expiryThreshold;
		}

		public String getPkiMount() {
			return this.pkiMount;
		}

		public void setPkiMount(String pkiMount) {
			this.pkiMount = pkiMount;
		}

		public @Nullable CertificateStore getStore() {
			return this.store;
		}

		public void setStore(@Nullable CertificateStore store) {
			this.store = store;
		}

	}

	/**
	 * Properties for the certificate store to cache certificates between application
	 * starts.
	 */
	public static class CertificateStore {

		/**
		 * Whether to enable caching of certificates using a versioned key/value secret
		 * backend.
		 */
		private boolean enabled = false;

		/**
		 * Path to the certificate store used to store the certificate bundle.
		 */
		private String path = "secret";

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

	public static class VaultSslBundleSupport {

		/**
		 * Options for the SSL connection.
		 */
		private @Nullable Options options;

		/**
		 * SSL Protocol to use.
		 */
		private String protocol = SslBundle.DEFAULT_PROTOCOL;

		public Options getOptions() {
			return this.options;
		}

		SslOptions getSslOptions() {
			return (this.options != null) ? SslOptions.of(this.options.getCiphers(), this.options.getEnabledProtocols())
					: SslOptions.NONE;
		}

		public void setOptions(@Nullable Options options) {
			this.options = options;
		}

		public String getProtocol() {
			return this.protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

	}

	/**
	 * Properties for a Vault-managed SSL bundle.
	 */
	public static class VaultSslBundle extends VaultSslBundleSupport {

		/**
		 * Role name to request certificates from Vault.
		 */
		private String roleName;

		/**
		 * Common name for the certificate request.
		 */
		private String commonName;

		/**
		 * TTL duration for the certificate request.
		 */
		private @Nullable Duration ttl;

		/**
		 * Alternate CN names for additional host names.
		 */
		private List<String> altNames = new ArrayList<>();

		/**
		 * Requested IP Subject Alternative Names.
		 */
		private List<String> ipSans = new ArrayList<>();

		/**
		 * Requested URI Subject Alternative Names.
		 */
		private List<String> uriSans = new ArrayList<>();

		/**
		 * Specifies custom OID/UTF8-string Subject Alternative Names. These must match
		 * values specified on the role in {@literal allowed_other_sans}. The format is
		 * the same as OpenSSL: {@literal <oid>;<type>:<value>} where the only current
		 * valid type is UTF8.
		 */
		private List<String> otherSans = new ArrayList<>();

		public String getRoleName() {
			return this.roleName;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		public String getCommonName() {
			return this.commonName;
		}

		public void setCommonName(String commonName) {
			this.commonName = commonName;
		}

		public @Nullable Duration getTtl() {
			return this.ttl;
		}

		public void setTtl(Duration ttl) {
			this.ttl = ttl;
		}

		public List<String> getAltNames() {
			return this.altNames;
		}

		public void setAltNames(List<String> altNames) {
			this.altNames = altNames;
		}

		public List<String> getIpSans() {
			return this.ipSans;
		}

		public void setIpSans(List<String> ipSans) {
			this.ipSans = ipSans;
		}

		public List<String> getUriSans() {
			return this.uriSans;
		}

		public void setUriSans(List<String> uriSans) {
			this.uriSans = uriSans;
		}

		public List<String> getOtherSans() {
			return this.otherSans;
		}

		public void setOtherSans(List<String> otherSans) {
			this.otherSans = otherSans;
		}

	}

	/**
	 * Properties for a Vault CA Certificate SSL bundles exposing only the Issuer
	 * Certificate in the trust store.
	 */
	public static class VaultIssuerCertificateBundle extends VaultSslBundleSupport {

		private String issuer = "default";

		public String getIssuer() {
			return this.issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}

	}

}
