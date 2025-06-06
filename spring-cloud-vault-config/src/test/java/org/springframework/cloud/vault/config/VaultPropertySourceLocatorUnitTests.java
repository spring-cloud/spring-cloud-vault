/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultPropertySourceLocator}.
 *
 * @author Ryan Hoegg
 * @author Mark Paluch
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class VaultPropertySourceLocatorUnitTests {

	private VaultPropertySourceLocator propertySourceLocator;

	@Mock
	private VaultConfigTemplate operations;

	@Mock
	private ConfigurableEnvironment configurableEnvironment;

	private VaultKeyValueBackendProperties properties = new VaultKeyValueBackendProperties();

	@BeforeEach
	public void before() {
		this.propertySourceLocator = new VaultPropertySourceLocator(this.operations, new VaultProperties(),
				VaultPropertySourceLocatorSupport.createConfiguration(this.properties));
	}

	@Test
	public void getOrderShouldReturnConfiguredOrder() {

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.getConfig().setOrder(42);

		this.propertySourceLocator = new VaultPropertySourceLocator(this.operations, vaultProperties,
				VaultPropertySourceLocatorSupport.createConfiguration(new VaultKeyValueBackendProperties()));

		assertThat(this.propertySourceLocator.getOrder()).isEqualTo(42);
	}

	@Test
	public void shouldLocateOnePropertySourceWithEmptyProfiles() {

		PropertySource<?> propertySource = this.propertySourceLocator.locate(this.configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).hasSize(1);
	}

	@Test
	public void shouldLocatePropertySourcesForActiveProfilesInDefaultContext() {

		this.properties.setProfiles(Arrays.asList("vermillion", "periwinkle"));

		PropertySource<?> propertySource = this.propertySourceLocator.locate(this.configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).extracting("name")
			.containsSequence("secret/application/periwinkle", "secret/application/vermillion");
	}

	@Test
	public void shouldLocatePropertySourcesInVaultApplicationContext() {

		VaultKeyValueBackendProperties backendProperties = new VaultKeyValueBackendProperties();
		backendProperties.setApplicationName("wintermute");
		backendProperties.setProfiles(Arrays.asList("vermillion", "periwinkle"));

		this.propertySourceLocator = new VaultPropertySourceLocator(this.operations, new VaultProperties(),
				VaultPropertySourceLocatorSupport.createConfiguration(backendProperties));

		PropertySource<?> propertySource = this.propertySourceLocator.locate(this.configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).extracting("name")
			.containsSequence("secret/wintermute/periwinkle", "secret/wintermute/vermillion", "secret/wintermute");
	}

	@Test
	public void shouldLocatePropertySourcesInEachPathSpecifiedWhenApplicationNameContainsSeveral() {

		VaultKeyValueBackendProperties backendProperties = new VaultKeyValueBackendProperties();
		backendProperties.setApplicationName("wintermute,straylight,icebreaker/armitage");
		backendProperties.setProfiles(Arrays.asList("vermillion", "periwinkle"));

		this.propertySourceLocator = new VaultPropertySourceLocator(this.operations, new VaultProperties(),
				VaultPropertySourceLocatorSupport.createConfiguration(backendProperties));

		PropertySource<?> propertySource = this.propertySourceLocator.locate(this.configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).extracting("name")
			.contains("secret/wintermute", "secret/straylight", "secret/icebreaker/armitage",
					"secret/wintermute/vermillion", "secret/wintermute/periwinkle", "secret/straylight/vermillion",
					"secret/straylight/periwinkle", "secret/icebreaker/armitage/vermillion",
					"secret/icebreaker/armitage/periwinkle");
	}

	@Test
	public void shouldCreatePropertySourcesInOrder() {

		DefaultSecretBackendConfigurer configurer = new DefaultSecretBackendConfigurer();
		configurer.add(new MySecondSecretBackendMetadata());
		configurer.add(new MyFirstSecretBackendMetadata());

		this.propertySourceLocator = new VaultPropertySourceLocator(this.operations, new VaultProperties(), configurer);

		PropertySource<?> propertySource = this.propertySourceLocator.locate(this.configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).extracting("name").containsSequence("foo", "bar");
	}

	@Order(1)
	static class MyFirstSecretBackendMetadata extends SecretBackendMetadataSupport {

		@Override
		public String getPath() {
			return "foo";
		}

	}

	@Order(2)
	static class MySecondSecretBackendMetadata extends SecretBackendMetadataSupport {

		@Override
		public String getPath() {
			return "bar";
		}

	}

}
