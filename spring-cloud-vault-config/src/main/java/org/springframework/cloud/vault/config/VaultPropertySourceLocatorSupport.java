/*
 * Copyright 2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.vault.config.GenericSecretBackendMetadata.create;

/**
 * Abstract {@link PropertySourceLocator} to create {@link PropertySource}s based on
 * {@link VaultGenericBackendProperties} and {@link SecretBackendMetadata}.
 *
 * @author Mark Paluch
 */
public abstract class VaultPropertySourceLocatorSupport implements PropertySourceLocator {

	private final String propertySourceName;

	private final VaultGenericBackendProperties genericBackendProperties;

	private final Collection<SecretBackendMetadata> backendAccessors;

	/**
	 * Creates a new {@link VaultPropertySourceLocatorSupport}.
	 *
	 * @param propertySourceName must not be {@literal null} or empty.
	 * @param genericBackendProperties must not be {@literal null}.
	 * @param backendAccessors must not be {@literal null}.
	 */
	public VaultPropertySourceLocatorSupport(String propertySourceName,
			VaultGenericBackendProperties genericBackendProperties,
			Collection<SecretBackendMetadata> backendAccessors) {

		Assert.hasText(propertySourceName, "PropertySource name must not be empty");
		Assert.notNull(backendAccessors, "BackendAccessors must not be null");
		Assert.notNull(genericBackendProperties,
				"VaultGenericBackendProperties must not be null");

		this.propertySourceName = propertySourceName;
		this.backendAccessors = backendAccessors;
		this.genericBackendProperties = genericBackendProperties;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {

		CompositePropertySource propertySource = createCompositePropertySource(
				environment);
		initialize(propertySource);

		return propertySource;
	}

	// -------------------------------------------------------------------------
	// Implementation hooks and helper methods
	// -------------------------------------------------------------------------

	/**
	 * Allows initialization the {@link PropertySource} before use. Implementations may
	 * override this method to preload properties in the {@link PropertySource}.
	 *
	 * @param propertySource must not be {@literal null}.
	 */
	protected void initialize(CompositePropertySource propertySource) {
	}

	/**
	 * Creates a {@link CompositePropertySource}.
	 *
	 * @param environment must not be {@literal null}.
	 * @return
	 */
	protected CompositePropertySource createCompositePropertySource(
			Environment environment) {

		List<PropertySource<?>> propertySources = doCreatePropertySources(environment);

		return doCreateCompositePropertySource(propertySourceName, propertySources);
	}

	/**
	 * Create {@link PropertySource}s given {@link Environment} from the property
	 * configuration.
	 *
	 * @param environment must not be {@literal null}.
	 * @return a {@link List} of ordered {@link PropertySource}s.
	 */
	protected List<PropertySource<?>> doCreatePropertySources(Environment environment) {

		List<PropertySource<?>> propertySources = new ArrayList<>();

		if (genericBackendProperties.isEnabled()) {
			propertySources.addAll(doCreateGenericPropertySources(environment));
		}

		for (SecretBackendMetadata backendAccessor : backendAccessors) {

			PropertySource<?> vaultPropertySource = createVaultPropertySource(
					backendAccessor);
			propertySources.add(vaultPropertySource);
		}

		return propertySources;
	}

	/**
	 * Create {@link PropertySource}s using the generic {@literal secret} backend.
	 * Property sources for the generic secret backend derive from the application name
	 * and active profiles to generate context paths.
	 *
	 * @param environment must not be {@literal null}.
	 * @return
	 */
	protected List<PropertySource<?>> doCreateGenericPropertySources(
			Environment environment) {

		List<PropertySource<?>> propertySources = new ArrayList<>();
		List<String> contexts = GenericSecretBackendMetadata
				.buildContexts(genericBackendProperties, environment);

		for (String propertySourceContext : contexts) {

			if (StringUtils.hasText(propertySourceContext)) {

				PropertySource<?> vaultPropertySource = createVaultPropertySource(create(
						genericBackendProperties.getBackend(), propertySourceContext));

				propertySources.add(vaultPropertySource);
			}
		}

		return propertySources;
	}

	/**
	 * Create a {@link CompositePropertySource} given a {@link List} of
	 * {@link PropertySource}s.
	 *
	 * @param propertySourceName the property source name.
	 * @param propertySources the property sources.
	 * @return the {@link CompositePropertySource} to use.
	 */
	protected CompositePropertySource doCreateCompositePropertySource(
			String propertySourceName, List<PropertySource<?>> propertySources) {

		CompositePropertySource compositePropertySource = new CompositePropertySource(
				propertySourceName);

		for (PropertySource<?> propertySource : propertySources) {
			compositePropertySource.addPropertySource(propertySource);
		}

		return compositePropertySource;
	}

	/**
	 * Create {@link VaultPropertySource} initialized with a
	 * {@link SecretBackendMetadata}.
	 *
	 * @param accessor the {@link SecretBackendMetadata}.
	 * @return the {@link VaultPropertySource} to use.
	 */
	protected abstract PropertySource<?> createVaultPropertySource(
			SecretBackendMetadata accessor);

}
