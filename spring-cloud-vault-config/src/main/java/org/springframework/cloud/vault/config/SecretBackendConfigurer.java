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

import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Helps to configure {@link SecretBackendMetadata secret backends} with support for
 * {@link PropertyTransformer property transformers}.
 *
 * <p>
 * Assists configuration with a fluent style. This configurer allows configuration via
 * context paths and direct registration of {@link SecretBackendMetadata}.
 * <p>
 * Use {@link #registerDefaultGenericSecretBackends(boolean)} to register default generic
 * secret backend property sources and
 * {@link #registerDefaultDiscoveredSecretBackends(boolean)} to register additional secret
 * backend property sources such as MySQL and RabbitMQ.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see PropertyTransformer
 * @see SecretBackendMetadata
 */
public interface SecretBackendConfigurer {

	/**
	 * Add a {@link SecretBackendMetadata} given its {@code path}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer add(String path);

	/**
	 * Add a {@link SecretBackendMetadata} given its {@code path} and
	 * {@link PropertyTransformer}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param propertyTransformer must not be {@literal null}.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer add(String path, PropertyTransformer propertyTransformer);

	/**
	 * Add a {@link SecretBackendMetadata}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer add(SecretBackendMetadata metadata);

	/**
	 * Add a {@link SecretBackendMetadata} given {@link RequestedSecret}. Property sources
	 * supporting leasing will derive lease renewal/rotation from
	 * {@link RequestedSecret.Mode}.
	 *
	 * @param requestedSecret must not be {@literal null} or empty.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer add(RequestedSecret requestedSecret);

	/**
	 * Add a {@link SecretBackendMetadata} given {@link RequestedSecret} and
	 * {@link PropertyTransformer}. Property sources supporting leasing will derive lease
	 * renewal/rotation from {@link RequestedSecret.Mode}.
	 *
	 * @param requestedSecret must not be {@literal null} or empty.
	 * @param propertyTransformer must not be {@literal null}.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer add(RequestedSecret requestedSecret,
			PropertyTransformer propertyTransformer);

	/**
	 * Register default generic secret backend property sources.
	 *
	 * @param registerDefault {@literal true} to enable default generic secret backend
	 * registration.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer registerDefaultGenericSecretBackends(boolean registerDefault);

	/**
	 * Register default discovered secret backend property sources from
	 * {@link SecretBackendMetadata} via {@link VaultSecretBackendDescriptor} beans.
	 *
	 * @param registerDefault {@literal true} to enable default discovered secret backend
	 * registration via {@link VaultSecretBackendDescriptor} beans.
	 * @return {@code this} {@link SecretBackendConfigurer}.
	 */
	SecretBackendConfigurer registerDefaultDiscoveredSecretBackends(
			boolean registerDefault);
}
