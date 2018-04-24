/*
 * Copyright 2018 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.core.util.PropertyTransformers;

/**
 * {@link SecretBackendMetadata} for the {@code kv} (key-value) secret backend.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class KeyValueSecretBackendMetadata extends SecretBackendMetadataSupport implements
		SecretBackendMetadata {

	private final String path;
	private final PropertyTransformer propertyTransformer;

	KeyValueSecretBackendMetadata(String path) {
		this(path, PropertyTransformers.noop());
	}

	private KeyValueSecretBackendMetadata(String path,
			PropertyTransformer propertyTransformer) {

		Assert.hasText(path, "Secret backend path must not be empty");
		Assert.notNull(propertyTransformer, "PropertyTransformer must not be null");

		this.path = path;
		this.propertyTransformer = propertyTransformer;
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code kv} secret backend given a
	 * {@code secretBackendPath} and {@code key}. Use plain mount and key paths. The
	 * required {@code data} segment is added by this method.
	 *
	 * @param secretBackendPath the secret backend mount path without leading/trailing
	 * slashes and without the {@code data} path segment, must not be empty or
	 * {@literal null}.
	 * @param key the key within the secret backend. May contain slashes but not
	 * leading/trailing slashes, must not be empty or {@literal null}.
	 * @return the {@link SecretBackendMetadata}
	 */
	public static SecretBackendMetadata create(String secretBackendPath, String key) {

		Assert.hasText(secretBackendPath, "Secret backend path must not be null or empty");
		Assert.hasText(key, "Key must not be null or empty");

		return create(String.format("%s/data/%s", secretBackendPath, key),
				UnwrappingPropertyTransformer.unwrap("data"));
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code generic} secret backend given
	 * a {@code path}.
	 *
	 * @param path the relative path of the secret. slashes, must not be empty or
	 * {@literal null}.
	 * @return the {@link SecretBackendMetadata}
	 */
	public static SecretBackendMetadata create(String path) {
		return new KeyValueSecretBackendMetadata(path, PropertyTransformers.noop());
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code generic} secret backend given
	 * a {@code path}.
	 *
	 * @param path the relative path of the secret. slashes, must not be empty or
	 * {@literal null}.
	 * @param propertyTransformer property transformer.
	 * @return the {@link SecretBackendMetadata}
	 */
	public static SecretBackendMetadata create(String path,
			PropertyTransformer propertyTransformer) {
		return new KeyValueSecretBackendMetadata(path, propertyTransformer);
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public PropertyTransformer getPropertyTransformer() {
		return propertyTransformer;
	}

	/**
	 * Build a list of context paths from application name and the active profile names.
	 * Application name and profiles support multiple (comma-separated) values.
	 *
	 * @param properties
	 * @param profiles active application profiles.
	 * @return list of context paths.
	 */
	public static List<String> buildContexts(
			VaultKeyValueBackendPropertiesSupport properties, List<String> profiles) {

		String appName = properties.getApplicationName();
		Set<String> contexts = new LinkedHashSet<>();

		String defaultContext = properties.getDefaultContext();
		contexts.addAll(buildContexts(defaultContext, profiles,
				properties.getProfileSeparator()));

		for (String applicationName : StringUtils.commaDelimitedListToSet(appName)) {
			contexts.addAll(buildContexts(applicationName, profiles,
					properties.getProfileSeparator()));
		}

		List<String> result = new ArrayList<>(contexts);

		Collections.reverse(result);

		return result;
	}

	/**
	 * Create a list of context names from a combination of application name and
	 * application name with profile name. Using an empty application name will return an
	 * empty list.
	 *
	 * @param applicationName the application name. May be empty.
	 * @param profiles active application profiles.
	 * @param profileSeparator profile separator character between application name and
	 * profile name.
	 * @return list of context names.
	 */
	public static List<String> buildContexts(String applicationName,
			List<String> profiles, String profileSeparator) {

		List<String> contexts = new ArrayList<>();

		if (!StringUtils.hasText(applicationName)) {
			return contexts;
		}

		if (!contexts.contains(applicationName)) {
			contexts.add(applicationName);
		}

		for (String profile : profiles) {

			if (!StringUtils.hasText(profile)) {
				continue;
			}

			String contextName = applicationName + profileSeparator + profile.trim();

			if (!contexts.contains(contextName)) {
				contexts.add(contextName);
			}
		}

		return contexts;
	}

	/**
	 * {@link PropertyTransformer} that strips a prefix from property names.
	 */
	static class UnwrappingPropertyTransformer implements PropertyTransformer {

		private final String prefixToStrip;

		private UnwrappingPropertyTransformer(String prefixToStrip) {

			Assert.notNull(prefixToStrip, "Property name prefix must not be null");

			this.prefixToStrip = prefixToStrip;
		}

		/**
		 * Create a new {@link PropertyTransformers.KeyPrefixPropertyTransformer} that
		 * adds a prefix to each key name.
		 * @param propertyNamePrefix the property name prefix to be added in front of each
		 * property name, must not be {@literal null}.
		 * @return a new {@link PropertyTransformers.KeyPrefixPropertyTransformer} that
		 * adds a prefix to each key name.
		 */
		public static PropertyTransformer unwrap(String propertyNamePrefix) {
			return new UnwrappingPropertyTransformer(propertyNamePrefix);
		}

		@Override
		public Map<String, Object> transformProperties(Map<String, ? extends Object> input) {

			Map<String, Object> target = new LinkedHashMap<>(input.size(), 1);

			for (Entry<String, ? extends Object> entry : input.entrySet()) {

				if (entry.getKey().startsWith(prefixToStrip + ".")) {
					target.put(entry.getKey().substring(prefixToStrip.length() + 1),
							entry.getValue());
				}
			}

			return target;
		}
	}
}
