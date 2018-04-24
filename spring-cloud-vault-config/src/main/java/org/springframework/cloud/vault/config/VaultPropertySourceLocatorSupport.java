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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

import static org.springframework.cloud.vault.config.GenericSecretBackendMetadata.*;

/**
 * Abstract {@link PropertySourceLocator} to create {@link PropertySource}s based on
 * {@link VaultGenericBackendProperties} and {@link SecretBackendMetadata}.
 *
 * @author Mark Paluch
 */
public abstract class VaultPropertySourceLocatorSupport implements PropertySourceLocator {

	private final String propertySourceName;

	private final PropertySourceLocatorConfiguration propertySourceLocatorConfiguration;

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

		this(propertySourceName,
				createConfiguration(genericBackendProperties, backendAccessors));
	}

	/**
	 * Creates a new {@link VaultPropertySourceLocatorSupport} given a
	 * {@link PropertySourceLocatorConfiguration}.
	 *
	 * @param propertySourceName must not be {@literal null} or empty.
	 * @param propertySourceLocatorConfiguration must not be {@literal null}.
	 * @since 1.1
	 */
	public VaultPropertySourceLocatorSupport(String propertySourceName,
			PropertySourceLocatorConfiguration propertySourceLocatorConfiguration) {

		Assert.hasText(propertySourceName, "PropertySource name must not be empty");
		Assert.notNull(propertySourceLocatorConfiguration,
				"PropertySourceLocatorConfiguration must not be null");

		this.propertySourceName = propertySourceName;
		this.propertySourceLocatorConfiguration = propertySourceLocatorConfiguration;
	}

	static PropertySourceLocatorConfiguration createConfiguration(
			VaultGenericBackendProperties genericBackendProperties,
			Collection<SecretBackendMetadata> backendAccessors) {

		Assert.notNull(genericBackendProperties,
				"VaultGenericBackendProperties must not be null");
		Assert.notNull(backendAccessors, "BackendAccessors must not be null");

		GenericPropertySourceLocatorConfiguration generic = new GenericPropertySourceLocatorConfiguration(
				genericBackendProperties);

		WrappedPropertySourceLocatorConfiguration backends = new WrappedPropertySourceLocatorConfiguration(
				new ArrayList<>(backendAccessors));

		return new CompositePropertySourceConfiguration(generic, backends);
	}

	static PropertySourceLocatorConfiguration createConfiguration(
			VaultGenericBackendProperties genericBackendProperties) {

		Assert.notNull(genericBackendProperties,
				"VaultGenericBackendProperties must not be null");

		return new GenericPropertySourceLocatorConfiguration(genericBackendProperties);
	}

	@Override
	public PropertySource<?> locate(Environment environment) {

		if (propertySourceLocatorConfiguration instanceof EnvironmentAware) {
			((EnvironmentAware) propertySourceLocatorConfiguration)
					.setEnvironment(environment);
		}

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

		Collection<SecretBackendMetadata> secretBackends = propertySourceLocatorConfiguration
				.getSecretBackends();
		List<SecretBackendMetadata> sorted = new ArrayList<>(secretBackends);
		List<PropertySource<?>> propertySources = new ArrayList<>();

		AnnotationAwareOrderComparator.sort(sorted);

		propertySources.addAll(doCreateGenericPropertySources(environment));

		for (SecretBackendMetadata backendAccessor : sorted) {

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
		return new ArrayList<>();
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

	@RequiredArgsConstructor
	private static class GenericPropertySourceLocatorConfiguration
			implements EnvironmentAware, PropertySourceLocatorConfiguration {

		private final VaultKeyValueBackendPropertiesSupport genericBackendProperties;

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {

			if (genericBackendProperties.isEnabled()) {

				List<String> contexts = GenericSecretBackendMetadata.buildContexts(
						genericBackendProperties,
						Arrays.asList(environment.getActiveProfiles()));

				List<SecretBackendMetadata> result = new ArrayList<>(contexts.size());

				for (String context : contexts) {
					result.add(create(genericBackendProperties.getBackend(), context));
				}

				return result;
			}

			return Collections.emptyList();
		}
	}

	@RequiredArgsConstructor
	private static class WrappedPropertySourceLocatorConfiguration
			implements PropertySourceLocatorConfiguration {

		private final List<SecretBackendMetadata> metadata;

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {
			return metadata;
		}
	}

	private static class CompositePropertySourceConfiguration
			implements PropertySourceLocatorConfiguration, EnvironmentAware {

		private final List<PropertySourceLocatorConfiguration> configurations;

		public CompositePropertySourceConfiguration(
				PropertySourceLocatorConfiguration... configurations) {

			List<PropertySourceLocatorConfiguration> copy = new ArrayList<>(
					Arrays.asList(configurations));

			AnnotationAwareOrderComparator.sortIfNecessary(copy);

			this.configurations = copy;
		}

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {

			List<SecretBackendMetadata> result = new ArrayList<>();

			for (PropertySourceLocatorConfiguration configuration : configurations) {
				result.addAll(configuration.getSecretBackends());
			}

			return result;
		}

		@Override
		public void setEnvironment(Environment environment) {

			for (PropertySourceLocatorConfiguration configuration : configurations) {
				if (configuration instanceof EnvironmentAware) {
					((EnvironmentAware) configuration).setEnvironment(environment);
				}
			}
		}
	}
}
