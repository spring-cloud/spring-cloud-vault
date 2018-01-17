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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.util.Assert;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.core.util.PropertyTransformers;

/**
 * Default {@link SecretBackendConfigurer} implementation that exposes its configuration
 * through {@link PropertySourceLocatorConfiguration}.
 *
 * @author Mark Paluch
 */
class DefaultSecretBackendConfigurer
		implements SecretBackendConfigurer, PropertySourceLocatorConfiguration {

	private final Map<String, SecretBackendMetadata> secretBackends = new LinkedHashMap<>();

	private boolean registerDefaultGenericSecretBackends = false;

	private boolean registerDefaultDiscoveredSecretBackends = false;

	@Override
	public SecretBackendConfigurer add(String path) {

		Assert.hasLength(path, "Path must not be empty");

		return add(path, PropertyTransformers.noop());
	}

	@Override
	public SecretBackendConfigurer add(String path,
			PropertyTransformer propertyTransformer) {

		Assert.hasLength(path, "Path must not be empty");
		Assert.notNull(propertyTransformer, "PropertyTransformer must not be null");

		return add(createMetadata(path, propertyTransformer));
	}

	private SimpleSecretBackendMetadata createMetadata(String path,
			PropertyTransformer propertyTransformer) {
		return new SimpleSecretBackendMetadata(path, propertyTransformer);
	}

	@Override
	public SecretBackendConfigurer add(SecretBackendMetadata metadata) {

		Assert.notNull(metadata, "SecretBackendMetadata must not be null");

		secretBackends.put(metadata.getPath(), metadata);

		return this;
	}

	@Override
	public SecretBackendConfigurer add(RequestedSecret requestedSecret) {

		Assert.notNull(requestedSecret, "RequestedSecret must not be null");

		return add(requestedSecret, PropertyTransformers.noop());
	}

	@Override
	public SecretBackendConfigurer add(RequestedSecret requestedSecret,
			PropertyTransformer propertyTransformer) {

		Assert.notNull(requestedSecret, "RequestedSecret must not be null");
		Assert.notNull(propertyTransformer, "PropertyTransformer must not be null");

		secretBackends.put(requestedSecret.getPath(),
				new SimpleLeasingSecretBackendMetadata(
						createMetadata(requestedSecret.getPath(), propertyTransformer),
						requestedSecret.getMode()));

		return this;
	}

	@Override
	public SecretBackendConfigurer registerDefaultGenericSecretBackends(
			boolean registerDefault) {

		this.registerDefaultGenericSecretBackends = registerDefault;

		return this;
	}

	@Override
	public SecretBackendConfigurer registerDefaultDiscoveredSecretBackends(
			boolean registerDefault) {

		this.registerDefaultDiscoveredSecretBackends = registerDefault;

		return this;
	}

	public boolean isRegisterDefaultGenericSecretBackends() {
		return registerDefaultGenericSecretBackends;
	}

	public boolean isRegisterDefaultDiscoveredSecretBackends() {
		return registerDefaultDiscoveredSecretBackends;
	}

	@Override
	public List<SecretBackendMetadata> getSecretBackends() {
		return new ArrayList<>(secretBackends.values());
	}

	@RequiredArgsConstructor
	private static class SimpleSecretBackendMetadata implements SecretBackendMetadata {

		private final String path;

		private final PropertyTransformer propertyTransformer;

		@Override
		public String getName() {
			return String.format("Context backend: %s", path);
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public PropertyTransformer getPropertyTransformer() {
			return propertyTransformer;
		}

		@Override
		public Map<String, String> getVariables() {
			return Collections.singletonMap("path", path);
		}
	}

	private static class SimpleLeasingSecretBackendMetadata
			extends SecretBackendMetadataWrapper implements LeasingSecretBackendMetadata {

		private final Mode mode;

		SimpleLeasingSecretBackendMetadata(SecretBackendMetadata delegate, Mode mode) {

			super(delegate);
			this.mode = mode;
		}

		@Override
		public Mode getLeaseMode() {
			return mode;
		}
	}
}
