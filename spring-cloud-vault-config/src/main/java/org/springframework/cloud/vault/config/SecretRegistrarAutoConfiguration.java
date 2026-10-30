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

package org.springframework.cloud.vault.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.lease.SecretRegistrar;
import org.springframework.vault.core.lease.SecretsRegistry;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SecretRegistrar}
 * registration.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@AutoConfigureAfter(VaultAutoConfiguration.class)
public class SecretRegistrarAutoConfiguration implements InitializingBean {

	private final ObjectProvider<SecretRegistrar> secretRegistrars;

	private final ObjectProvider<SecretsRegistry> secretsRegistry;

	public SecretRegistrarAutoConfiguration(ObjectProvider<SecretRegistrar> secretRegistrars,
			ObjectProvider<SecretsRegistry> secretsRegistry) {
		this.secretRegistrars = secretRegistrars;
		this.secretsRegistry = secretsRegistry;
	}

	@Override
	public void afterPropertiesSet() {

		this.secretsRegistry.ifUnique(it -> {
			this.secretRegistrars.forEach(registrar -> registrar.registerSecret(it));
		});
	}

}
