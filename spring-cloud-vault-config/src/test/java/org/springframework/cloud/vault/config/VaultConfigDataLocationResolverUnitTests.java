/*
 * Copyright 2019-2021 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VaultConfigDataLocationResolver}.
 *
 * @author Mark Paluch
 * @author Jeffrey van der Laan
 * @author Benjamin Bargeton
 */
public class VaultConfigDataLocationResolverUnitTests {

	ConfigDataLocationResolverContext contextMock = mock(ConfigDataLocationResolverContext.class);

	Profiles profilesMock = mock(Profiles.class);

	DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	@Before
	public void before() {
		when(this.contextMock.getBootstrapContext()).thenReturn(this.bootstrapContext);
		when(this.contextMock.getBinder()).thenReturn(new Binder());
	}

	@Test
	public void shouldDiscoverDefaultLocations() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		when(this.profilesMock.getActive()).thenReturn(Arrays.asList("a", "b"));

		assertThat(
				resolver.resolveProfileSpecific(this.contextMock, ConfigDataLocation.of("vault:"), this.profilesMock))
			.hasSize(3);

		assertThat(
				resolver.resolveProfileSpecific(this.contextMock, ConfigDataLocation.of("vault://"), this.profilesMock))
			.hasSize(3);
	}

	@Test
	public void shouldRejectLocationWithTrailingSlash() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		assertThatIllegalArgumentException()
			.isThrownBy(() -> resolver.resolveProfileSpecific(this.contextMock, ConfigDataLocation.of("vault://foo/"),
					this.profilesMock))
			.withMessage("Location 'vault://foo/' must not end with a trailing slash");
	}

	@Test
	public void shouldDiscoverContextualLocations() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		List<VaultConfigLocation> locations = resolver.resolveProfileSpecific(this.contextMock,
				ConfigDataLocation.of("vault://my/context/path"), this.profilesMock);

		assertThat(locations).hasSize(1);
		assertThat(locations.get(0)).hasToString("VaultConfigLocation [path='my/context/path', optional=false]");
		assertThat(locations.get(0)
			.getSecretBackendMetadata()
			.getPropertyTransformer()
			.transformProperties(Collections.singletonMap("key", "value"))).containsEntry("key", "value");
	}

	@Test
	public void shouldDiscoverContextualLocationsWithPrefix() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		List<VaultConfigLocation> locations = resolver.resolveProfileSpecific(this.contextMock,
				ConfigDataLocation.of("vault://my/context/path?prefix=myPrefix."), this.profilesMock);

		assertThat(locations).hasSize(1);
		assertThat(locations.get(0)
			.getSecretBackendMetadata()
			.getPropertyTransformer()
			.transformProperties(Collections.singletonMap("key", "value"))).containsEntry("myPrefix.key", "value");
	}

	@Test
	public void shouldNotPrefixWhenPrefixIsEmpty() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		List<VaultConfigLocation> locations = resolver.resolveProfileSpecific(this.contextMock,
				ConfigDataLocation.of("vault://my/context/path?prefix="), this.profilesMock);

		assertThat(locations).hasSize(1);
		assertThat(locations.get(0)
			.getSecretBackendMetadata()
			.getPropertyTransformer()
			.transformProperties(Collections.singletonMap("key", "value"))).containsEntry("key", "value");
	}

	@Test
	public void kvProfilesPropertyPrecedenceShouldBeRespected() {

		VaultConfigDataLocationResolver resolver = new VaultConfigDataLocationResolver();

		when(this.profilesMock.getActive()).thenReturn(Arrays.asList("a", "b"));
		when(this.contextMock.getBinder()).thenReturn(new Binder(ConfigurationPropertySource.from(
				new MapPropertySource("test", Collections.singletonMap("spring.cloud.vault.kv.profiles", "c, d, e")))));

		assertThat(
				resolver.resolveProfileSpecific(this.contextMock, ConfigDataLocation.of("vault://"), this.profilesMock))
			.hasSize(4);
	}

}
