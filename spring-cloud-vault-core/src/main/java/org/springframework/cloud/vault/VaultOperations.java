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
package org.springframework.cloud.vault;

import java.net.URI;
import java.util.Map;

/**
 * Interface that specified a basic set of Vault operations, implemented by
 * {@link VaultTemplate}.
 *
 * @author Mark Paluch
 */
public interface VaultOperations {

	/**
	 * Executes a Vault {@link SessionCallback}. Allows to interact with Vault in an
	 * authenticated session.
	 *
	 * @param path the path of the resource, e.g. {@code transit/encrypt/foo}, must not be
	 * empty or {@literal null}.
	 * @param sessionCallback the request.
	 * @return
	 */
	<T> T doWithVault(String path, SessionCallback sessionCallback);

	/**
	 * Executes a Vault {@link SessionCallback}. Allows to interact with Vault in an
	 * authenticated session.
	 *
	 * @param pathTemplate the path of the resource, e.g. {@code transit/ key}/foo}, must
	 * not be empty or {@literal null}. * @param variables the variables for expansion of
	 * the {@code pathTemplate}, must not be {@literal null}.
	 * @param sessionCallback the request.
	 * @return
	 */
	<T> T doWithVault(String pathTemplate, Map<String, ?> variables,
			SessionCallback sessionCallback);

	/**
	 * Query the current Vault service for it's health status.
	 *
	 * @return A {@link VaultHealthResponse} containing the current service status.
	 */
	VaultHealthResponse health();

	/**
	 * Callback to execute actions within an authenticated {@link VaultSession}.
	 *
	 * @author Mark Paluch
	 */
	public interface SessionCallback {

		/**
		 * Callback method.
		 *
		 * @param uri the URI that is used for the request, must not be {@literal null}.
		 * @param session session to use, must not be {@literal null}.
		 * @return
		 */
		<T> T doWithVault(URI uri, VaultSession session);
	}

	/**
	 * An authenticated Vault session.
	 *
	 * @author Mark Paluch
	 */
	public interface VaultSession {

		/**
		 * Read data from the given Vault {@code uri}.
		 *
		 * @param uri must not be {@literal null}.
		 * @return the {@link VaultClientResponse}.
		 */
		public VaultClientResponse read(URI uri);

		/**
		 * Write data to the given Vault {@code uri}.
		 *
		 * @param uri must not be {@literal null}.
		 * @param entity must not be {@literal null}.
		 * @return the {@link VaultClientResponse}.
		 */
		public VaultClientResponse write(URI uri, Object entity);
	}
}
