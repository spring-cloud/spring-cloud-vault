/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.vault.config;

import java.net.URI;

import org.springframework.cloud.vault.config.VaultProperties.Ssl;
import org.springframework.util.StringUtils;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;

/**
 * Support class for Vault configuration providing utility methods.
 * 
 * @author Mark Paluch
 * @since 2.1
 */
class VaultConfigurationUtil {

	/**
	 * Create a {@link SslConfiguration} given {@link Ssl SSL properties}.
	 * 
	 * @param ssl
	 * @return
	 */
	static SslConfiguration createSslConfiguration(Ssl ssl) {

		if (ssl == null) {
			return SslConfiguration.unconfigured();
		}

		KeyStoreConfiguration keyStore = KeyStoreConfiguration.unconfigured();
		KeyStoreConfiguration trustStore = KeyStoreConfiguration.unconfigured();

		if (ssl.getKeyStore() != null) {
			if (StringUtils.hasText(ssl.getKeyStorePassword())) {
				keyStore = KeyStoreConfiguration.of(ssl.getKeyStore(), ssl
						.getKeyStorePassword().toCharArray());
			}
			else {
				keyStore = KeyStoreConfiguration.of(ssl.getKeyStore());
			}
		}

		if (ssl.getTrustStore() != null) {

			if (StringUtils.hasText(ssl.getTrustStorePassword())) {
				trustStore = KeyStoreConfiguration.of(ssl.getTrustStore(), ssl
						.getTrustStorePassword().toCharArray());
			}
			else {
				trustStore = KeyStoreConfiguration.of(ssl.getTrustStore());
			}
		}

		return new SslConfiguration(keyStore, trustStore);
	}

	/**
	 * Create a {@link VaultEndpoint} given {@link VaultProperties}.
	 * 
	 * @param vaultProperties
	 * @return
	 */
	static VaultEndpoint createVaultEndpoint(VaultProperties vaultProperties) {

		if (StringUtils.hasText(vaultProperties.getUri())) {
			return VaultEndpoint.from(URI.create(vaultProperties.getUri()));
		}

		VaultEndpoint vaultEndpoint = new VaultEndpoint();
		vaultEndpoint.setHost(vaultProperties.getHost());
		vaultEndpoint.setPort(vaultProperties.getPort());
		vaultEndpoint.setScheme(vaultProperties.getScheme());

		return vaultEndpoint;
	}
}
