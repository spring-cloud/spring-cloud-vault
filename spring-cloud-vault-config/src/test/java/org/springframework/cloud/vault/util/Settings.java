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

import java.io.File;

import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.core.io.FileSystemResource;

/**
 * Utility to retrieve settings during test.
 * 
 * @author Mark Paluch
 */
public class Settings {

	/**
	 * @return the vault properties.
	 */
	public static VaultProperties createVaultProperties() {

		File workDir = findWorkDir(new File(System.getProperty("user.dir")));

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.getSsl().setTrustStorePassword("changeit");
		vaultProperties.getSsl().setTrustStore(new FileSystemResource(new File(workDir, "keystore.jks")));
		vaultProperties.setToken(token().getToken());

		return vaultProperties;
	}

	private static File findWorkDir(File file) {

		File searchLevel = file;
		while (searchLevel.getParentFile() != null && searchLevel.getParentFile() != searchLevel) {

			File work = new File(searchLevel, "work");
			if (work.isDirectory() && work.exists()) {
				return work;
			}

			searchLevel = searchLevel.getParentFile();
		}

		throw new IllegalStateException(
				String.format("Cannot find work directory in %s or any parent directories", file.getAbsoluteFile()));
	}

	/**
	 * @return the token to use during tests.
	 */
	public static VaultToken token() {
		return VaultToken.of(System.getProperty("vault.token", "00000000-0000-0000-0000-000000000000"));
	}
}
