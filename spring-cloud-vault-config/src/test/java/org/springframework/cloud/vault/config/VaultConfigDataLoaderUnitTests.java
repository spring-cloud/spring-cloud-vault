/*
 * Copyright 2020-2021 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.cloud.vault.util.IntegrationTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultConfigDataLoader}.
 *
 * @author Mark Paluch
 */
public class VaultConfigDataLoaderUnitTests extends IntegrationTestSupport {

	@Test
	public void shouldConsiderDisabledVault() {

		VaultConfigDataLoader loader = new VaultConfigDataLoader(new DeferredLogs());
		DefaultBootstrapContext context = new DefaultBootstrapContext();

		VaultProperties properties = new VaultProperties();
		properties.setEnabled(false);

		context.register(VaultProperties.class, it -> properties);

		ConfigData vaultDisabled = loader.load(() -> context, new VaultConfigLocation("foo", true));

		assertThat(vaultDisabled).isNull();
	}

}
