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

package org.springframework.cloud.vault.integration;

import org.junit.Test;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.cloud.vault.util.PrepareVault;
import org.springframework.cloud.vault.util.Settings;

/**
 * Integration tests for {@link PrepareVault}.
 * 
 * @author Mark Paluch
 */
public class PrepareVaultTests {

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private PrepareVault prepareVault = new PrepareVault(new TestRestTemplate());

	@Test
	public void initializeShouldCreateANewVault() throws Exception {

		prepareVault.setRootToken(Settings.token());
		prepareVault.setVaultProperties(vaultProperties);

		if (!prepareVault.isAvailable()) {
			VaultToken rootToken = prepareVault.initializeVault();
			prepareVault.setRootToken(rootToken);
			prepareVault.createToken(vaultProperties.getToken(), "root");
		}
	}
}
