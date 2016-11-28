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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link LeasingVaultPropertySourceLocator}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class LeasingVaultPropertySourceLocatorUnitTests {

	private LeasingVaultPropertySourceLocator propertySourceLocator;

	@Mock
	private VaultConfigTemplate operations;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ConfigurableEnvironment configurableEnvironment;

	@Mock
	private LeasingVaultPropertySource leasingVaultPropertySource;

    @Mock
    private VaultPropertySourceContextStrategy vaultPropertySourceContextStrategy;

	@Before
	public void before() {

		propertySourceLocator = new LeasingVaultPropertySourceLocator(operations,
				new VaultProperties(), new VaultGenericBackendProperties(), 
				vaultPropertySourceContextStrategy,
				Collections.<SecretBackendMetadata> emptyList(), taskScheduler);
	}

	@Test
	public void getOrderShouldReturnConfiguredOrder() {

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.getConfig().setOrder(10);

		propertySourceLocator = new LeasingVaultPropertySourceLocator(operations,
				vaultProperties, new VaultGenericBackendProperties(),
				vaultPropertySourceContextStrategy,
				Collections.<SecretBackendMetadata> emptyList(), taskScheduler);

		assertThat(propertySourceLocator.getOrder()).isEqualTo(10);
	}

	@Test
	public void shouldLocatePropertySources() {

		when(configurableEnvironment.getActiveProfiles()).thenReturn(new String[0]);
		when(vaultPropertySourceContextStrategy.buildContexts(configurableEnvironment))
			.thenReturn(Collections.singletonList("/secret/appName"));

		PropertySource<?> propertySource = propertySourceLocator
				.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).hasSize(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldDispose() {

		Set set = (Set) ReflectionTestUtils.getField(propertySourceLocator,
				"locatedPropertySources");
		set.add(leasingVaultPropertySource);

		propertySourceLocator.destroy();

		verify(leasingVaultPropertySource).destroy();
	}
}