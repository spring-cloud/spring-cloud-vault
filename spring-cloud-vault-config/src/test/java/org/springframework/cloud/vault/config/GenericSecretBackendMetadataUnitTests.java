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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GenericSecretBackendMetadata}.
 *
 * @author Mark Paluch
 */
public class GenericSecretBackendMetadataUnitTests {

	VaultGenericBackendProperties properties = new VaultGenericBackendProperties();

	@Test
	public void shouldCreateDefaultContexts() {

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Collections.emptyList());

		assertThat(contexts).hasSize(1).contains("application");
	}

	@Test
	public void shouldCreateDefaultForAppNameAndDefaultContext() {

		properties.setApplicationName("my-app");

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Collections.emptyList());

		assertThat(contexts).hasSize(2).containsSequence("my-app", "application");
	}

	@Test
	public void shouldCreateDefaultForAppNameAndDefaultContextWithProfiles() {

		properties.setApplicationName("my-app");

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Arrays.asList("cloud", "local"));

		assertThat(contexts).hasSize(6).containsSequence("my-app/local", "my-app/cloud",
				"my-app", "application/local", "application/cloud", "application");
	}

	@Test
	public void shouldCreateAppNameContextIfDefaultIsDisabled() {

		properties.setApplicationName("my-app");
		properties.setDefaultContext("");

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Collections.emptyList());

		assertThat(contexts).hasSize(1).containsSequence("my-app");
	}

	@Test
	public void shouldCreateContextsForCommaSeparatedAppName() {

		properties.setApplicationName("foo,bar");

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Collections.emptyList());

		assertThat(contexts).hasSize(3).containsSequence("bar", "foo", "application");
	}

	@Test
	public void shouldCreateContextsWithProfile() {

		properties.setApplicationName("foo,bar");

		List<String> contexts = GenericSecretBackendMetadata.buildContexts(properties,
				Arrays.asList("cloud", "local"));

		assertThat(contexts).hasSize(9).containsSequence("bar/local", "bar/cloud", "bar",
				"foo/local", "foo/cloud", "foo", "application/local", "application/cloud",
				"application");
	}
}
