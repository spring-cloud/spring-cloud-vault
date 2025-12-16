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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.config.VaultAutoConfiguration;
import org.springframework.cloud.vault.config.VaultAutoConfiguration.TaskSchedulerWrapper;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.cloud.vault.config.VaultReactiveAutoConfiguration;
import org.springframework.cloud.vault.ssl.VaultSslBundlesProperties.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.certificate.CertificateAuthority;
import org.springframework.vault.core.certificate.CertificateBundleStore;
import org.springframework.vault.core.certificate.CertificateContainer;
import org.springframework.vault.core.certificate.PersistentCertificateAuthority;
import org.springframework.vault.core.certificate.VaultCertificateAuthority;
import org.springframework.vault.core.certificate.VersionedCertificateBundleStore;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Vault-managed SSL bundles.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@AutoConfiguration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ VaultProperties.class, VaultSslBundlesProperties.class })
@AutoConfigureAfter({ VaultAutoConfiguration.class, VaultReactiveAutoConfiguration.class })
@ConditionalOnSingleCandidate(VaultOperations.class)
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class VaultSslBundleAutoConfiguration {

	private final VaultSslBundlesProperties bundles;

	public VaultSslBundleAutoConfiguration(VaultSslBundlesProperties bundles) {
		this.bundles = bundles;
	}

	@Bean
	PropertiesVaultSslBundlesRegistrar propertiesVaultSslBundlesRegistrar() {
		return new PropertiesVaultSslBundlesRegistrar(this.bundles);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.cloud.vault.ssl.lifecycle.store.enabled", havingValue = "true")
	public VersionedCertificateBundleStore certificateBundleStore(VaultOperations vaultTemplate) {
		return new VersionedCertificateBundleStore(vaultTemplate, this.bundles.getLifecycle().getStore().getPath());
	}

	@Bean
	@ConditionalOnMissingBean
	public CertificateAuthority vaultCertificateAuthority(VaultOperations vaultTemplate,
			ObjectProvider<CertificateBundleStore> certificateBundleStore) {
		Lifecycle lifecycle = this.bundles.getLifecycle();
		VaultCertificateAuthority ca = new VaultCertificateAuthority(vaultTemplate.opsForPki(lifecycle.getPkiMount()));
		CertificateBundleStore store = certificateBundleStore.getIfUnique();
		return store != null ? new PersistentCertificateAuthority(store, ca, lifecycle.getExpiryThreshold()) : ca;
	}

	@Bean
	@ConditionalOnMissingBean
	public CertificateContainer certificateContainer(TaskSchedulerWrapper taskSchedulerWrapper,
			CertificateAuthority certificateAuthority) {
		Lifecycle lifecycle = this.bundles.getLifecycle();
		CertificateContainer container = new CertificateContainer(certificateAuthority,
				taskSchedulerWrapper.getTaskScheduler());
		container.setExpiryThreshold(lifecycle.getExpiryThreshold());
		container.afterPropertiesSet();
		container.start();
		return container;
	}

	@Bean
	VaultCertificateContainerSslBundleRegistrar vaultSslBundleRegistrar(
			ObjectProvider<VaultSslBundleRegistrar> registrars, CertificateContainer certificateContainer) {
		DefaultVaultSslBundleRegistry vaultSslBundleRegistry = new DefaultVaultSslBundleRegistry();
		registrars.orderedStream().forEach(registrar -> registrar.register(vaultSslBundleRegistry));
		return new VaultCertificateContainerSslBundleRegistrar(certificateContainer, vaultSslBundleRegistry);
	}

}
