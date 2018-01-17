/*
 * Copyright 2016-2018 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LeasingVaultPropertySourceLocator}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class LeasingVaultPropertySourceLocatorUnitTests {

	private LeasingVaultPropertySourceLocator propertySourceLocator;

	@Mock
	private ConfigurableEnvironment configurableEnvironment;

	@Mock
	private SecretLeaseContainer secretLeaseContainer;

	@Before
	public void before() {

		propertySourceLocator = new LeasingVaultPropertySourceLocator(
				new VaultProperties(), VaultPropertySourceLocatorSupport
						.createConfiguration(new VaultGenericBackendProperties()),
				secretLeaseContainer);
	}

	@Test
	public void getOrderShouldReturnConfiguredOrder() {

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.getConfig().setOrder(10);

		propertySourceLocator = new LeasingVaultPropertySourceLocator(vaultProperties,
				VaultPropertySourceLocatorSupport.createConfiguration(
						new VaultGenericBackendProperties()),
				secretLeaseContainer);

		assertThat(propertySourceLocator.getOrder()).isEqualTo(10);
	}

	@Test
	public void shouldLocatePropertySources() {

		when(configurableEnvironment.getActiveProfiles()).thenReturn(new String[0]);

		PropertySource<?> propertySource = propertySourceLocator
				.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).hasSize(1);
		verify(secretLeaseContainer)
				.addRequestedSecret(RequestedSecret.rotating("secret/application"));
	}

	@Test
	public void shouldLocateLeaseAwareSources() {

		RequestedSecret rotating = RequestedSecret.rotating("secret/rotating");
		DefaultSecretBackendConfigurer configurer = new DefaultSecretBackendConfigurer();
		configurer.add(rotating);
		configurer.add("database/mysql/creds/readonly");

		propertySourceLocator = new LeasingVaultPropertySourceLocator(
				new VaultProperties(), configurer, secretLeaseContainer);

		PropertySource<?> propertySource = propertySourceLocator
				.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		verify(secretLeaseContainer).addRequestedSecret(rotating);
		verify(secretLeaseContainer).addRequestedSecret(
				RequestedSecret.renewable("database/mysql/creds/readonly"));
	}
}
