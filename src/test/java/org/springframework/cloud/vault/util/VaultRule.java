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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.rules.ExternalResource;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;

/**
 * Vault rule to ensure a running and prepared Vault.
 * 
 * @author Mark Paluch
 */
public class VaultRule extends ExternalResource {

	private final VaultProperties vaultProperties;
	private final PrepareVault prepareVault = new PrepareVault(new TestRestTemplate());

	public VaultRule() {
		this(Settings.createVaultProperties());
	}

	public VaultRule(VaultProperties vaultProperties) {
		this.vaultProperties = vaultProperties;
	}

	@Override
	public void before() {

		try (Socket socket = new Socket()) {

			socket.connect(new InetSocketAddress(InetAddress.getByName("localhost"),
					vaultProperties.getPort()));
			socket.close();

		}
		catch (Exception ex) {
			throw new IllegalStateException(String.format(
					"Vault is not running on localhost:%d which is required to run a test using @Rule %s",
					vaultProperties.getPort(), getClass().getSimpleName()));
		}

		prepareVault.setVaultProperties(vaultProperties);

		if (!prepareVault.isAvailable()) {
			VaultToken rootToken = prepareVault.initializeVault();
			prepareVault.setRootToken(rootToken);
			prepareVault.createToken(vaultProperties.getToken(), "root");
		}
		else {
			prepareVault.setRootToken(Settings.token());
		}
	}

	public PrepareVault prepare() {
		return prepareVault;
	}

}
