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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySourceLocator} using {@link VaultClient}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
public class VaultPropertySourceLocator implements PropertySourceLocator {

	private VaultClient vaultClient;

	private VaultProperties properties;
	private final Collection<SecureBackendAccessor> backendAcessors;

	private transient final VaultState vaultState = new VaultState();

	/**
	 * Creates a new {@link VaultPropertySourceLocator}.
	 * @param vaultClient must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @param backendAccessors must not be {@literal null}.
	 */
	public VaultPropertySourceLocator(VaultClient vaultClient, VaultProperties properties,
			Collection<SecureBackendAccessor> backendAccessors) {

		Assert.notNull(vaultClient, "VaultClient must not be null");
		Assert.notNull(properties, "VaultProperties must not be null");
		Assert.notNull(backendAccessors, "BackendAccessors must not be null");

		this.vaultClient = vaultClient;
		this.properties = properties;
		this.backendAcessors = backendAccessors;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {

		if (environment instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
			String appName = env.getProperty("spring.application.name");
			List<String> profiles = Arrays.asList(env.getActiveProfiles());

			List<String> contexts = new ArrayList<>();

			String defaultContext = this.properties.getDefaultContext();
			contexts.add(defaultContext);
			addProfiles(contexts, defaultContext, profiles);

			String baseContext = appName;
			contexts.add(baseContext);
			addProfiles(contexts, baseContext, profiles);

			Collections.reverse(contexts);

			CompositePropertySource composite = new CompositePropertySource("vault");

			for (String propertySourceContext : contexts) {

				if(StringUtils.hasText(propertySourceContext)) {
					VaultPropertySource propertySource = create(propertySourceContext);
					propertySource.init(backendAcessors);
					composite.addPropertySource(propertySource);
				}
			}

			return composite;
		}
		return null;
	}

	private VaultPropertySource create(String context) {
		return new VaultPropertySource(context, this.vaultClient, this.properties,
				this.vaultState);
	}

	private void addProfiles(List<String> contexts, String baseContext,
			List<String> profiles) {
		for (String profile : profiles) {
			contexts.add(baseContext + this.properties.getProfileSeparator() + profile);
		}
	}
}