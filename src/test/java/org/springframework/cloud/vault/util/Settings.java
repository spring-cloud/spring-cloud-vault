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

package org.springframework.cloud.vault.util;

import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;

/**
 * Utility to retrieve settings during test.
 * 
 * @author Mark Paluch
 */
public class Settings {

	/**
	 * 
	 * @return the vault properties.
	 */
	public static VaultProperties createVaultProperties() {

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.setToken(token().getToken());
		vaultProperties.setHost(System.getProperty("vault.host", "localhost"));

		return vaultProperties;
	}

	/**
	 * @return the token to use during tests.
	 */
	public static VaultToken token() {
		return VaultToken.of(System.getProperty("vault.token",
				"00000000-0000-0000-0000-000000000000"));
	}
}
