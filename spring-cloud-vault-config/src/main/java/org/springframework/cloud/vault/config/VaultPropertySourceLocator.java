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

import static org.springframework.cloud.vault.config.GenericSecretBackendMetadata.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySourceLocator} using {@link VaultConfigTemplate}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Jean-Philippe Bélanger
 */
class VaultPropertySourceLocator implements PropertySourceLocator, PriorityOrdered {

	private final VaultConfigOperations operations;
	private final VaultProperties properties;
	private final VaultGenericBackendProperties genericBackendProperties;
	private final Collection<SecretBackendMetadata> backendAccessors;

	/**
	 * Creates a new {@link VaultPropertySourceLocator}.
	 * 
	 * @param operations must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param genericBackendProperties must not be {@literal null}.
	 * @param backendAccessors must not be {@literal null}.
	 */
	public VaultPropertySourceLocator(VaultConfigOperations operations,
			VaultProperties properties,
			VaultGenericBackendProperties genericBackendProperties,
			Collection<SecretBackendMetadata> backendAccessors) {

		Assert.notNull(operations, "VaultConfigOperations must not be null");
		Assert.notNull(properties, "VaultProperties must not be null");
		Assert.notNull(backendAccessors, "BackendAccessors must not be null");
		Assert.notNull(genericBackendProperties,
				"VaultGenericBackendProperties must not be null");

		this.operations = operations;
		this.properties = properties;
		this.backendAccessors = backendAccessors;
		this.genericBackendProperties = genericBackendProperties;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {

		if (environment instanceof ConfigurableEnvironment) {

			CompositePropertySource propertySource = createCompositePropertySource(
					(ConfigurableEnvironment) environment);
			initialize(propertySource);

			return propertySource;
		}
		return null;
	}

	@Override
	public int getOrder() {
		return properties.getConfig().getOrder();
	}

	private List<String> buildContexts(ConfigurableEnvironment env) {

		String appName = genericBackendProperties.getApplicationName();
		List<String> profiles = Arrays.asList(env.getActiveProfiles());
		List<String> contexts = new ArrayList<>();

		String defaultContext = genericBackendProperties.getDefaultContext();
		if (StringUtils.hasText(defaultContext)) {
			contexts.add(defaultContext);
		}

		addProfiles(contexts, defaultContext, profiles);

		if (StringUtils.hasText(appName)) {

			if (!contexts.contains(appName)) {
				contexts.add(appName);
			}

			addProfiles(contexts, appName, profiles);
		}

		Collections.reverse(contexts);
		return contexts;
	}

	private CompositePropertySource createCompositePropertySource(
			ConfigurableEnvironment environment) {

		List<PropertySource<?>> propertySources = new ArrayList<>();

		if (genericBackendProperties.isEnabled()) {

			List<String> contexts = buildContexts(environment);
			for (String propertySourceContext : contexts) {

				if (StringUtils.hasText(propertySourceContext)) {

					VaultPropertySource vaultPropertySource = createVaultPropertySource(
							create(genericBackendProperties.getBackend(),
									propertySourceContext));

					propertySources.add(vaultPropertySource);
				}
			}
		}

		for (SecretBackendMetadata backendAccessor : backendAccessors) {

			VaultPropertySource vaultPropertySource = createVaultPropertySource(
					backendAccessor);
			propertySources.add(vaultPropertySource);
		}

		return doCreateCompositePropertySource(propertySources);
	}

	// -------------------------------------------------------------------------
	// Implementation hooks and helper methods
	// -------------------------------------------------------------------------

	/**
	 * Create a {@link CompositePropertySource} given a {@link List} of
	 * {@link PropertySource}s.
	 *
	 * @param propertySources the property sources.
	 * @return the {@link CompositePropertySource} to use.
	 */
	protected CompositePropertySource doCreateCompositePropertySource(
			List<PropertySource<?>> propertySources) {

		CompositePropertySource compositePropertySource = new CompositePropertySource(
				"vault");

		for (PropertySource<?> propertySource : propertySources) {
			compositePropertySource.addPropertySource(propertySource);
		}

		return compositePropertySource;
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
	protected VaultPropertySource createVaultPropertySource(
			SecretBackendMetadata accessor) {
		return new VaultPropertySource(this.operations, this.properties.isFailFast(),
				accessor);
	}

	private void addProfiles(List<String> contexts, String baseContext,
			List<String> profiles) {

		for (String profile : profiles) {
			String context = baseContext
					+ this.genericBackendProperties.getProfileSeparator() + profile;

			if (!contexts.contains(context)) {
				contexts.add(context);
			}
		}
	}
}
