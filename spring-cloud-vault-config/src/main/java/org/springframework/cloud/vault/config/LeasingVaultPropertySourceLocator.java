/*
 * Copyright 2016 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Extension to {@link LeasingVaultPropertySourceLocator} that creates
 * {@link LeasingVaultPropertySource}s.
 *
 * @author Mark Paluch
 * @see LeasingVaultPropertySource
 */
@CommonsLog
class LeasingVaultPropertySourceLocator extends VaultPropertySourceLocator
		implements DisposableBean {

	private final VaultConfigOperations operations;

	private final VaultProperties properties;

	private final TaskScheduler taskScheduler;

	private final Set<PropertySource<?>> locatedPropertySources = new HashSet<>();

	/**
	 * Creates a new {@link LeasingVaultPropertySourceLocator}.
	 * @param operations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param genericBackendProperties must not be {@literal null}.
	 * @param vaultPropertySourceContextStrategy must not be {@literal null}.
	 * @param backendAccessors must not be {@literal null}.
	 * @param taskScheduler must not be {@literal null}.
	 */
	public LeasingVaultPropertySourceLocator(VaultConfigOperations operations,
			VaultProperties properties,
			VaultGenericBackendProperties genericBackendProperties,
			VaultPropertySourceContextStrategy vaultPropertySourceContextStrategy,
			Collection<SecretBackendMetadata> backendAccessors,
			TaskScheduler taskScheduler) {

		super(operations, properties, genericBackendProperties, vaultPropertySourceContextStrategy, backendAccessors);

		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		Assert.notNull(operations, "VaultConfigTemplate must not be null");
		Assert.notNull(properties, "VaultProperties must not be null");

		this.operations = operations;
		this.properties = properties;
		this.taskScheduler = taskScheduler;
	}

	@Override
	protected VaultPropertySource createVaultPropertySource(
			SecretBackendMetadata accessor) {

		LeasingVaultPropertySource propertySource = new LeasingVaultPropertySource(
				this.operations, this.properties.isFailFast(), accessor, taskScheduler);

		locatedPropertySources.add(propertySource);

		return propertySource;
	}

	@Override
	public void destroy() {

		Set<PropertySource<?>> propertySources = new HashSet<>(locatedPropertySources);

		for (PropertySource<?> propertySource : propertySources) {

			locatedPropertySources.remove(propertySource);

			if (propertySource instanceof LeasingVaultPropertySource) {

				try {
					((LeasingVaultPropertySource) propertySource).destroy();
				}
				catch (Exception e) {
					log.warn(String.format("Cannot destroy property source %s",
							propertySource.getName()), e);
				}
			}
		}
	}
}
