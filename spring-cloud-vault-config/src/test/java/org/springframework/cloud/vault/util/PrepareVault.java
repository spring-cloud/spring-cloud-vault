/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.cloud.vault.util;

import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultSysOperations;
import org.springframework.vault.support.VaultHealth;
import org.springframework.vault.support.VaultInitializationRequest;
import org.springframework.vault.support.VaultInitializationResponse;
import org.springframework.vault.support.VaultMount;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.VaultTokenRequest;
import org.springframework.vault.support.VaultTokenRequest.VaultTokenRequestBuilder;
import org.springframework.vault.support.VaultTokenResponse;
import org.springframework.vault.support.VaultUnsealStatus;

/**
 * @author Mark Paluch
 */
public class PrepareVault {

	private final VaultOperations vaultOperations;

	private final VaultSysOperations adminOperations;

	public PrepareVault(VaultOperations vaultOperations) {

		this.vaultOperations = vaultOperations;
		this.adminOperations = vaultOperations.opsForSys();
	}

	/**
	 * Initialize Vault and unseal the vault.
	 * @return the root token.
	 */
	public VaultToken initializeVault() {

		int createKeys = 2;
		int requiredKeys = 2;

		VaultInitializationResponse initialized = this.vaultOperations.opsForSys()
			.initialize(VaultInitializationRequest.create(createKeys, requiredKeys));

		for (int i = 0; i < requiredKeys; i++) {

			VaultUnsealStatus unsealStatus = this.vaultOperations.opsForSys().unseal(initialized.getKeys().get(i));

			if (!unsealStatus.isSealed()) {
				break;
			}
		}

		return initialized.getRootToken();
	}

	/**
	 * Create a token for the given {@code tokenId} and {@code policy}.
	 * @param tokenId the must not be {@literal null}.
	 * @param policy the must not be {@literal null}.
	 * @return the token.
	 */
	public VaultToken createToken(String tokenId, String policy) {

		VaultTokenRequestBuilder builder = VaultTokenRequest.builder().id(tokenId);

		if (StringUtils.hasText(policy)) {
			builder.withPolicy(policy);
		}

		VaultTokenResponse vaultTokenResponse = this.vaultOperations.opsForToken().create(builder.build());
		return vaultTokenResponse.getToken();
	}

	/**
	 * Check whether Vault is available (vault created and unsealed).
	 * @return whether Vault is available.
	 */
	public boolean isAvailable() {
		return this.adminOperations.isInitialized() && !this.adminOperations.health().isSealed();
	}

	/**
	 * Mount an auth backend.
	 * @param authBackend the must not be {@literal null}.
	 */
	public void mountAuth(String authBackend) {

		Assert.hasText(authBackend, "AuthBackend must not be empty");

		this.adminOperations.authMount(authBackend, VaultMount.create(authBackend));
	}

	/**
	 * Check whether a auth-backend is enabled.
	 * @param authBackend the must not be {@literal null}.
	 * @return whether the backend is mounted.
	 */
	public boolean hasAuth(String authBackend) {

		Assert.hasText(authBackend, "AuthBackend must not be empty");

		return this.adminOperations.getAuthMounts().containsKey(authBackend + "/");
	}

	/**
	 * Mount an secret backend.
	 * @param secretBackend must not be {@literal null} or empty.
	 */
	public void mountSecret(String secretBackend) {
		mountSecret(secretBackend, secretBackend, Collections.emptyMap());
	}

	/**
	 * Mount an secret backend {@code secretBackend} at {@code path}.
	 * @param secretBackend must not be {@literal null} or empty.
	 * @param path must not be {@literal null} or empty.
	 * @param config must not be {@literal null}.
	 */
	public void mountSecret(String secretBackend, String path, Map<String, Object> config) {

		Assert.hasText(secretBackend, "SecretBackend must not be empty");
		Assert.hasText(path, "Mount path must not be empty");
		Assert.notNull(config, "Configuration must not be null");

		VaultMount mount = VaultMount.builder().type(secretBackend).config(config).build();
		this.adminOperations.mount(path, mount);
	}

	/**
	 * Check whether a auth-backend is enabled.
	 * @param secretBackend the must not be {@literal null}.
	 * @return whether the backend is mounted.
	 */
	public boolean hasSecretBackend(String secretBackend) {

		Assert.hasText(secretBackend, "SecretBackend must not be empty");
		Map<String, VaultMount> mounts = this.adminOperations.getMounts();
		return mounts.containsKey(secretBackend) || mounts.containsKey(secretBackend + "/");
	}

	public VaultOperations getVaultOperations() {
		return this.vaultOperations;
	}

	/**
	 * @return Vault version from the health check. Versions beginning from Vault 0.6.2
	 * will expose a version number.
	 */
	public Version getVersion() {

		VaultHealth health = getVaultOperations().opsForSys().health();

		if (StringUtils.hasText(health.getVersion())) {

			String version = health.getVersion();

			// Migration code for Vault 0.6.1
			if (version.startsWith("Vault v")) {
				version = version.substring(7);
			}

			return Version.parse(version);
		}

		return Version.parse("0.0.0");
	}

	/**
	 * Disable Vault versioning Key-Value backend (kv version 2).
	 */
	public void disableGenericVersioning() {

		this.vaultOperations.opsForSys().unmount("secret");

		VaultMount kv = VaultMount.builder().type("kv").config(Collections.singletonMap("versioned", false)).build();
		this.vaultOperations.opsForSys().mount("secret", kv);
	}

	public void mountVersionedKvBackend() {

		mountSecret("kv", "versioned", Collections.emptyMap());
		this.vaultOperations.write("sys/mounts/versioned/tune",
				Collections.singletonMap("options", Collections.singletonMap("version", "2")));
	}

}
