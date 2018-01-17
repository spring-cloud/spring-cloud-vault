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

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * Provides a convenient implementation of the {@link SecretBackendMetadata} interface
 * that can be subclassed to override specific methods.
 * <p/>
 * This class implements the Wrapper or Decorator pattern. Methods default to calling
 * through to the wrapped request object.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public class SecretBackendMetadataWrapper implements SecretBackendMetadata {

	private final SecretBackendMetadata delegate;

	/**
	 * Create a new {@link SecretBackendMetadataWrapper} given
	 * {@link SecretBackendMetadata}.
	 *
	 * @param delegate must not be {@literal null}.
	 */
	public SecretBackendMetadataWrapper(SecretBackendMetadata delegate) {

		Assert.notNull(delegate, "SecretBackendMetadata delegate must not be null");

		this.delegate = delegate;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public String getPath() {
		return delegate.getPath();
	}

	@Override
	public PropertyTransformer getPropertyTransformer() {
		return delegate.getPropertyTransformer();
	}

	@Override
	public Map<String, String> getVariables() {
		return delegate.getVariables();
	}
}
