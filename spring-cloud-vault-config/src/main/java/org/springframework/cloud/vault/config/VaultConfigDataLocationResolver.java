/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ConfigDataLocationResolver} for Vault resolving {@link VaultConfigLocation}
 * using the {@code vault:} prefix.
 *
 * @author Mark Paluch
 */
public class VaultConfigDataLocationResolver implements ConfigDataLocationResolver<VaultConfigLocation> {

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, String location) {

		boolean vaultEnabled = context.getBinder().bind(VaultProperties.PREFIX + ".enabled", Boolean.class)
				.orElse(true);

		return location.startsWith(VaultConfigLocation.VAULT_PREFIX) && vaultEnabled;
	}

	@Override
	public List<VaultConfigLocation> resolve(ConfigDataLocationResolverContext context, String location,
			boolean optional) throws ConfigDataLocationNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<VaultConfigLocation> resolveProfileSpecific(ConfigDataLocationResolverContext context, String location,
			boolean optional, Profiles profiles) throws ConfigDataLocationNotFoundException {

		context.getBootstrapContext().registerIfAbsent(VaultProperties.class,
				ignore -> context.getBinder().bindOrCreate(VaultProperties.PREFIX, VaultProperties.class));

		if (location.trim().equals(VaultConfigLocation.VAULT_PREFIX)) {

			List<VaultSecretBackendDescriptor> descriptors = findDescriptors(context);
			List<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories = (List) SpringFactoriesLoader
					.loadFactories(SecretBackendMetadataFactory.class, getClass().getClassLoader());

			PropertySourceLocatorConfigurationFactory factory = new PropertySourceLocatorConfigurationFactory(
					Collections.emptyList(), descriptors, factories);

			VaultKeyValueBackendProperties kvProperties = context.getBinder()
					.bindOrCreate(VaultKeyValueBackendProperties.PREFIX, VaultKeyValueBackendProperties.class);

			kvProperties.setApplicationName(getApplicationName(context.getBinder()));
			kvProperties.setProfiles(profiles.getActive());

			PropertySourceLocatorConfiguration configuration = factory
					.getPropertySourceConfiguration(Collections.singletonList(kvProperties));

			Collection<SecretBackendMetadata> secretBackends = configuration.getSecretBackends();
			List<SecretBackendMetadata> sorted = new ArrayList<>(secretBackends);
			AnnotationAwareOrderComparator.sort(sorted);

			return sorted.stream().map(it -> new VaultConfigLocation(it, optional)).collect(Collectors.toList());
		}

		String contextPath = location.substring(VaultConfigLocation.VAULT_PREFIX.length());

		return Collections.singletonList(new VaultConfigLocation(contextPath, optional));
	}

	private static String getApplicationName(Binder binder) {

		return binder.bind("spring.cloud.vault.application-name", String.class)
				.orElseGet(() -> binder.bind("spring.application-name", String.class).orElse(""));
	}

	private List<VaultSecretBackendDescriptor> findDescriptors(ConfigDataLocationResolverContext context) {

		List<String> descriptorClasses = SpringFactoriesLoader.loadFactoryNames(VaultSecretBackendDescriptor.class,
				getClass().getClassLoader());

		List<VaultSecretBackendDescriptor> descriptors = new ArrayList<>(descriptorClasses.size());

		try {
			for (String className : descriptorClasses) {

				Class<VaultSecretBackendDescriptor> descriptorClass = (Class<VaultSecretBackendDescriptor>) ClassUtils
						.forName(className, getClass().getClassLoader());

				MergedAnnotations annotations = MergedAnnotations.from(descriptorClass);
				if (annotations.isPresent(ConfigurationProperties.class)) {

					String prefix = annotations.get(ConfigurationProperties.class).getString("prefix");
					VaultSecretBackendDescriptor hydratedDescriptor = context.getBinder().bindOrCreate(prefix,
							descriptorClass);
					descriptors.add(hydratedDescriptor);
				}
				else {
					throw new IllegalStateException(String.format(
							"VaultSecretBackendDescriptor %s is not annotated with @ConfigurationProperties",
							className));
				}
			}
		}
		catch (ReflectiveOperationException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}

		return descriptors;
	}

}
