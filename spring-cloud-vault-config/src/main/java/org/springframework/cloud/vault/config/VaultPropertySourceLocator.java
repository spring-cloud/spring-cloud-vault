/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link PropertySourceLocator} using {@link VaultConfigTemplate}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Jean-Philippe Bélanger
 * @author Ryan Hoegg
 */
class VaultPropertySourceLocator extends VaultPropertySourceLocatorSupport
		implements PriorityOrdered {

	private final VaultConfigOperations operations;
	private final VaultProperties properties;

	/**
	 * Creates a new {@link VaultPropertySourceLocator}.
	 *
	 * @param operations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param propertySourceLocatorConfiguration must not be {@literal null}.
	 * @since 1.1
	 */
	public VaultPropertySourceLocator(VaultConfigOperations operations,
			VaultProperties properties,
			PropertySourceLocatorConfiguration propertySourceLocatorConfiguration) {

		super("vault", propertySourceLocatorConfiguration);

		Assert.notNull(operations, "VaultConfigOperations must not be null");
		Assert.notNull(properties, "VaultProperties must not be null");

		this.operations = operations;
		this.properties = properties;
	}

	@Override
	public int getOrder() {
		return properties.getConfig().getOrder();
	}

	/**
	 * Initialize nested {@link PropertySource}s inside the
	 * {@link CompositePropertySource}.
	 * @param propertySource the {@link CompositePropertySource} to initialize.
	 */
	protected void initialize(CompositePropertySource propertySource) {

		for (PropertySource<?> source : propertySource.getPropertySources()) {
			((VaultPropertySource) source).init();
		}
	}

	/**
	 * Create {@link VaultPropertySource} initialized with a
	 * {@link SecretBackendMetadata}.
	 *
	 * @param accessor the {@link SecretBackendMetadata}.
	 * @return the {@link VaultPropertySource} to use.
	 */
	protected PropertySource<?> createVaultPropertySource(
			SecretBackendMetadata accessor) {
		return new VaultPropertySource(this.operations, this.properties.isFailFast(),
				accessor);
	}
}
