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

import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Properties-based {@link VaultSslBundleRegistrar} implementation.
 *
 * @author Mark Paluch
 */
class PropertiesVaultSslBundlesRegistrar implements VaultSslBundleRegistrar {

	private final VaultSslBundlesProperties bundles;

	PropertiesVaultSslBundlesRegistrar(VaultSslBundlesProperties bundles) {
		this.bundles = bundles;
	}

	@Override
	public void register(VaultSslBundleRegistry registry) {

		this.bundles.getTrustBundle().forEach((name, properties) -> {
			registry.register(name, spec -> {
				return spec.trustAnchor(properties.getIssuer())
					.sslOptions(properties.getSslOptions())
					.sslProtocol(properties.getProtocol());
			});
		});

		this.bundles.getBundle().forEach((name, properties) -> {
			registry.register(name, spec -> {
				return spec.issueCertificate(properties.getRoleName())
					.sslOptions(properties.getSslOptions())
					.sslProtocol(properties.getProtocol())
					.request(it -> {
						PropertyMapper propertyMapper = PropertyMapper.get();
						propertyMapper.from(properties.getCommonName()).whenHasText().to(it::commonName);
						propertyMapper.from(properties.getAltNames()).to(it::altNames);
						propertyMapper.from(properties.getIpSans()).to(it::ipSubjectAltNames);
						propertyMapper.from(properties.getUriSans()).to(it::uriSubjectAltNames);
						propertyMapper.from(properties.getOtherSans()).to(it::otherSans);
						propertyMapper.from(properties.getTtl()).to(it::ttl);
						propertyMapper.from(properties.getCommonName()).whenHasText().to(it::commonName);
					});
			});
		});
	}

}
