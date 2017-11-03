/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Authentication options for {@link KubernetesAuthentication}.
 * <p>
 * Authentication options provide the path, role and jwt supplier.
 * {@link KubernetesAuthentication} can be constructed using {@link #builder()}. Instances
 * of this class are immutable once constructed.
 *
 * @author Michal Budzyn
 * @since 1.1
 * @see KubernetesAuthentication
 * @see #builder()
 */
class KubernetesAuthenticationOptions {

	static final String DEFAULT_KUBERNETES_AUTHENTICATION_PATH = "kubernetes";

	/**
	 * Path of the kubernetes authentication backend mount.
	 */
	private final String path;

	/**
	 * Name of the role against which the login is being attempted.
	 */
	private final String role;

	/**
	 * {@link KubernetesJwtSupplier} instance to obtain a service account JSON Web Tokens.
	 */
	private final KubernetesJwtSupplier jwtSupplier;

	private KubernetesAuthenticationOptions(String path, String role,
											KubernetesJwtSupplier jwtSupplier) {

		this.path = path;
		this.role = role;
		this.jwtSupplier = jwtSupplier;
	}

	/**
	 * @return a new {@link KubernetesAuthenticationOptionsBuilder}.
	 */
	static KubernetesAuthenticationOptionsBuilder builder() {
		return new KubernetesAuthenticationOptionsBuilder();
	}

	/**
	 * @return the path of the aws authentication backend mount.
	 */
	String getPath() {
		return path;
	}

	/**
	 * @return name of the role against which the login is being attempted.
	 */
	String getRole() {
		return role;
	}

	/**
	 * @return JSON Web Token supplier.
	 */
	KubernetesJwtSupplier getJwtSupplier() {
		return jwtSupplier;
	}

	/**
	 * Builder for {@link KubernetesAuthenticationOptions}.
	 */
	static class KubernetesAuthenticationOptionsBuilder {
		private String path = DEFAULT_KUBERNETES_AUTHENTICATION_PATH;

		private String role;

		private KubernetesJwtSupplier jwtSupplier;

		/**
		 * Configure the mount path.
		 *
		 * @param path must not be {@literal null} or empty.
		 * @return {@code this} {@link KubernetesAuthenticationOptionsBuilder}.
		 */
		KubernetesAuthenticationOptionsBuilder path(String path) {

			Assert.hasText(path, "Path must not be empty");

			this.path = path;
			return this;
		}

		/**
		 * Configure the role.
		 *
		 * @param role name of the role against which the login is being attempted, must
		 * not be {@literal null} or empty.
		 * @return {@code this} {@link KubernetesAuthenticationOptionsBuilder}.
		 */
		KubernetesAuthenticationOptionsBuilder role(String role) {

			Assert.hasText(role, "Role must not be empty");

			this.role = role;
			return this;
		}

		/**
		 * Configure the {@link KubernetesJwtSupplier} to obtain a Kubernetes authentication token.
		 *
		 * @param jwtSupplier the supplier, must not be {@literal null}.
		 * @return {@code this} {@link KubernetesAuthenticationOptionsBuilder}.
		 */
		KubernetesAuthenticationOptionsBuilder jwtSupplier(
				KubernetesJwtSupplier jwtSupplier) {

			Assert.notNull(jwtSupplier, "JwtSupplier must not be null");

			this.jwtSupplier = jwtSupplier;
			return this;
		}

		/**
		 * Build a new {@link KubernetesAuthenticationOptions} instance.
		 *
		 * @return a new {@link KubernetesAuthenticationOptions}.
		 */
		KubernetesAuthenticationOptions build() {

			Assert.notNull(role, "Role must not be null");

			return new KubernetesAuthenticationOptions(path, role,
					jwtSupplier == null ? new KubernetesServiceAccountTokenFile()
							: jwtSupplier);
		}
	}
}