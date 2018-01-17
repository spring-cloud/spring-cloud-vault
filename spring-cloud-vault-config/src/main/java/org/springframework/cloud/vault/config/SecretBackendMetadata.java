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

import java.util.Map;

import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Interface specifying the API to obtain URL variables and optionally a
 * {@link PropertyTransformer}. Typically used by {@link VaultPropertySource}. Supports
 * ordering of implementations.
 *
 * @author Mark Paluch
 * @see PropertyTransformer
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 */
public interface SecretBackendMetadata {

	/**
	 * Return a readable name of this secret backend.
	 *
	 * @return the name of this secret backend.
	 */
	String getName();

	/**
	 * Return the path of this secret backend.
	 *
	 * @return the path of this secret backend.
	 * @since 1.1
	 */
	String getPath();

	/**
	 * Return a {@link PropertyTransformer} to post-process properties retrieved from
	 * Vault.
	 *
	 * @return the property transformer.
	 * @see org.springframework.vault.core.util.PropertyTransformers
	 */
	PropertyTransformer getPropertyTransformer();

	/**
	 * @return URL template variables. URI variables should declare either {@code backend}
	 * and {@code key} or {@code path} properties.
	 */
	Map<String, String> getVariables();
}
