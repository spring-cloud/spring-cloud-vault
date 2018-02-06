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

import org.junit.Test;

import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.support.VaultToken;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link }ClientAuthenticationFactory}.
 *
 * @author Mark Paluch
 */
public class ClientAuthenticationFactoryUnitTests {

	@Test
	public void shouldSupportAppRoleRoleIdProvidedSecretIdProvided() {

		VaultProperties properties = new VaultProperties();
		properties.getAppRole().setRoleId("foo");
		properties.getAppRole().setSecretId("bar");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getRoleId()).isInstanceOf(RoleId.provided("foo").getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.provided("bar").getClass());
	}

	@Test
	public void shouldSupportAppRoleRoleIdProvidedSecretIdAbsent() {

		VaultProperties properties = new VaultProperties();
		properties.getAppRole().setRoleId("foo");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getRoleId()).isInstanceOf(RoleId.provided("foo").getClass());
		assertThat(options.getSecretId()).isInstanceOf(SecretId.absent().getClass());
	}

	@Test
	public void shouldSupportAppRoleRoleIdProvidedSecretIdPull() {

		VaultProperties properties = new VaultProperties();
		properties.setToken("token");
		properties.getAppRole().setRoleId("foo");
		properties.getAppRole().setRole("my-role");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getAppRole()).isEqualTo("my-role");
		assertThat(options.getRoleId()).isInstanceOf(RoleId.provided("foo").getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.pull(VaultToken.of("token")).getClass());
	}

	@Test
	public void shouldSupportAppRoleFullPull() {

		VaultProperties properties = new VaultProperties();
		properties.setToken("token");
		properties.getAppRole().setRole("my-role");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getAppRole()).isEqualTo("my-role");
		assertThat(options.getRoleId()).isInstanceOf(
				RoleId.pull(VaultToken.of("token")).getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.pull(VaultToken.of("token")).getClass());
	}

	@Test
	public void shouldSupportAppRoleFullWrapped() {

		VaultProperties properties = new VaultProperties();
		properties.setToken("token");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getRoleId()).isInstanceOf(
				RoleId.wrapped(VaultToken.of("token")).getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.wrapped(VaultToken.of("token")).getClass());
	}

	@Test
	public void shouldSupportAppRoleRoleIdWrappedSecretIdProvided() {

		VaultProperties properties = new VaultProperties();
		properties.setToken("token");
		properties.getAppRole().setSecretId("bar");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getRoleId()).isInstanceOf(
				RoleId.wrapped(VaultToken.of("token")).getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.provided("bar").getClass());
	}

	@Test
	public void shouldSupportAppRoleRoleIdProvidedSecretIdWrapped() {

		VaultProperties properties = new VaultProperties();
		properties.setToken("token");
		properties.getAppRole().setRoleId("foo");

		AppRoleAuthenticationOptions options = ClientAuthenticationFactory
				.getAppRoleAuthenticationOptions(properties);

		assertThat(options.getRoleId()).isInstanceOf(RoleId.provided("foo").getClass());
		assertThat(options.getSecretId()).isInstanceOf(
				SecretId.wrapped(VaultToken.of("token")).getClass());
	}

	@Test
	public void shouldRejectUnconfiguredRoleId() {

		VaultProperties properties = new VaultProperties();

		assertThatThrownBy(
				() -> ClientAuthenticationFactory
						.getAppRoleAuthenticationOptions(properties)).isInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	public void shouldRejectUnconfiguredRoleIdIfRoleNameSet() {

		VaultProperties properties = new VaultProperties();
		properties.getAppRole().setRole("my-role");

		assertThatThrownBy(
				() -> ClientAuthenticationFactory
						.getAppRoleAuthenticationOptions(properties)).isInstanceOf(
				IllegalArgumentException.class);
	}
}
