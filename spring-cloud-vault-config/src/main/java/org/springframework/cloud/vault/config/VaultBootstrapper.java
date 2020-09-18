/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.boot.Bootstrapper;
import org.springframework.util.Assert;

/**
 * Utility to customize Bootstrapping of Vault when using the ConfigData API when
 * importing {@code vault://}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public abstract class VaultBootstrapper {

	private VaultBootstrapper() {
	}

	/**
	 * Create a {@link Bootstrapper} that configures a {@link VaultConfigurer}.
	 * @param configurer the configurer to apply.
	 * @return the bootstrapper object.
	 */
	public static Bootstrapper fromConfigurer(VaultConfigurer configurer) {

		Assert.notNull(configurer, "VaultConfigurer must not be null");

		return registry -> registry.register(VaultConfigurer.class, context -> configurer);
	}

}
