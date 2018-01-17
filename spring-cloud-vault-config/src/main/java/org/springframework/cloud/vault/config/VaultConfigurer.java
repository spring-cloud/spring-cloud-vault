/*
 * Copyright 2017-2018 the original author or authors.
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

/**
 * Defines callback methods to customize the configuration for Spring Cloud Vault
 * applications.
 *
 * <p>
 * Configuration classes may implement this interface to be called back and given a chance
 * to customize the default configuration. Consider implementing this interface and
 * overriding the relevant methods for your needs.
 *
 * <p>
 * Registered bean instances of {@link VaultConfigurer} disable default secret backend
 * registration for the generic and integrative (other discovered
 * {@link SecretBackendMetadata}) backends. See
 * {@link SecretBackendConfigurer#registerDefaultGenericSecretBackends(boolean)} and
 * {@link SecretBackendConfigurer#registerDefaultDiscoveredSecretBackends(boolean)} for
 * more details.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see SecretBackendConfigurer
 */
public interface VaultConfigurer {

	/**
	 * Configure the secret backends that are instantiated as
	 * {@link org.springframework.core.env.PropertySource property sources}.
	 *
	 * @param configurer the {@link SecretBackendConfigurer} to configure secret backends,
	 * must not be {@literal null}.
	 */
	void addSecretBackends(SecretBackendConfigurer configurer);
}
