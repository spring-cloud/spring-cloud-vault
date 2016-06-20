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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultToken;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;

/**
 * Test helper to prepare various settings within Vault.
 * 
 * @author Mark Paluch
 */
public class PrepareVault {

	public final static String INITIALIZE_URL_TEMPLATE = "{baseuri}/sys/init";
	public final static String MOUNT_AUTH_URL_TEMPLATE = "{baseuri}/sys/auth/{authBackend}";
	public final static String SYS_AUTH_URL_TEMPLATE = "{baseuri}/sys/auth";
	public final static String MOUNT_SECRET_URL_TEMPLATE = "{baseuri}/sys/mounts/{type}";
	public final static String SYS_MOUNTS_URL_TEMPLATE = "{baseuri}/sys/mounts";
	public final static String SEAL_STATUS_URL_TEMPLATE = "{baseuri}/sys/seal-status";
	public final static String UNSEAL_URL_TEMPLATE = "{baseuri}/sys/unseal";
	public final static String CREATE_TOKEN_URL_TEMPLATE = "{baseuri}/auth/token/create-orphan";
	public final static String WRITE_URL_TEMPLATE = "{baseuri}/{path}";
	public static final ParameterizedTypeReference<Map<String, Object>> MAP_OF_MAPS_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {

	};

	private final RestTemplate restTemplate;

	@Setter
	@NonNull
	private VaultProperties vaultProperties;

	@Setter
	@NonNull
	private VaultToken rootToken;

	public PrepareVault(RestTemplate restTemplate) {

		Assert.notNull(restTemplate, "RestTemplate must not be null");
		this.restTemplate = restTemplate;
	}

	/**
	 * Initialize Vault and unseal the vault.
	 *
	 * @return the root token.
	 */
	public VaultToken initializeVault() {

		Assert.notNull(vaultProperties, "VaultProperties must not be null");

		Map<String, String> parameters = parameters(vaultProperties);

		int createKeys = 2;
		int requiredKeys = 2;

		InitializeVault initializeVault = InitializeVault.of(createKeys, requiredKeys);

		ResponseEntity<VaultInitialized> initResponse = restTemplate.exchange(
				INITIALIZE_URL_TEMPLATE, HttpMethod.PUT,
				new HttpEntity<>(initializeVault), VaultInitialized.class, parameters);

		if (!initResponse.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Cannot initialize vault: " + initResponse.toString());
		}
		VaultInitialized initialized = initResponse.getBody();

		for (int i = 0; i < requiredKeys; i++) {

			UnsealKey unsealKey = UnsealKey.of(initialized.getKeys().get(i));
			ResponseEntity<UnsealProgress> unsealResponse = restTemplate.exchange(
					UNSEAL_URL_TEMPLATE, HttpMethod.PUT, new HttpEntity<>(unsealKey),
					UnsealProgress.class, parameters);

			UnsealProgress unsealProgress = unsealResponse.getBody();
			if (!unsealProgress.isSealed()) {
				break;
			}
		}

		return VaultToken.of(initialized.getRootToken());
	}

	/**
	 * Create a token for the given {@code tokenId} and {@code policy}.
	 * 
	 * @param tokenId
	 * @param policy
	 * @return
	 */
	public VaultToken createToken(String tokenId, String policy) {

		Map<String, String> parameters = parameters(vaultProperties);

		CreateToken createToken = new CreateToken();
		createToken.setId(tokenId);
		if (policy != null) {
			createToken.setPolicies(Collections.singletonList(policy));
		}

		HttpHeaders headers = authenticatedHeaders();

		HttpEntity<CreateToken> entity = new HttpEntity<>(createToken, headers);

		ResponseEntity<TokenCreated> createTokenResponse = restTemplate.exchange(
				CREATE_TOKEN_URL_TEMPLATE, HttpMethod.POST, entity, TokenCreated.class,
				parameters);

		if (!createTokenResponse.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Cannot create token: " + createTokenResponse.toString());
		}

		AuthToken authToken = createTokenResponse.getBody().getAuth();

		return VaultToken.of(authToken.getClientToken());
	}

	/**
	 * Check whether Vault is available (vault created and unsealed).
	 *
	 * @return
	 */
	public boolean isAvailable() {

		Map<String, String> parameters = parameters(vaultProperties);

		ResponseEntity<String> exchange = restTemplate
				.getForEntity(SEAL_STATUS_URL_TEMPLATE, String.class, parameters);

		if (exchange.getStatusCode().is2xxSuccessful()) {
			return true;
		}

		if (exchange.getStatusCode().is4xxClientError()) {
			return false;
		}
		throw new IllegalStateException("Vault error: " + exchange.toString());
	}

	/**
	 * Mount an auth backend.
	 *
	 * @param authBackend
	 */
	public void mountAuth(String authBackend) {

		Assert.hasText(authBackend, "AuthBackend must not be empty");

		Map<String, String> parameters = parameters(vaultProperties);
		parameters.put("authBackend", authBackend);

		Map<String, String> requestEntity = Collections.singletonMap("type", authBackend);

		HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestEntity,
				authenticatedHeaders());

		ResponseEntity<String> responseEntity = restTemplate.exchange(
				MOUNT_AUTH_URL_TEMPLATE, HttpMethod.POST, entity, String.class,
				parameters);

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Cannot create mount auth backend: " + responseEntity.toString());
		}

		responseEntity.getBody();
	}

	/**
	 * Check whether a auth-backend is enabled.
	 * 
	 * @param authBackend
	 * @return
	 */
	public boolean hasAuth(String authBackend) {

		Assert.hasText(authBackend, "AuthBackend must not be empty");
		return hasMount(SYS_AUTH_URL_TEMPLATE, authBackend);
	}

	/**
	 * Mount an secret backend.
	 *
	 * @param secretBackend
	 */
	public void mountSecret(String secretBackend) {

		Assert.hasText(secretBackend, "SecretBackend must not be empty");

		Map<String, String> parameters = parameters(vaultProperties);
		parameters.put("type", secretBackend);

		Map<String, String> requestEntity = Collections.singletonMap("type",
				secretBackend);

		HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestEntity,
				authenticatedHeaders());

		ResponseEntity<String> responseEntity = restTemplate.exchange(
				MOUNT_SECRET_URL_TEMPLATE, HttpMethod.POST, entity, String.class,
				parameters);

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Cannot create mount secret backend: " + responseEntity.toString());
		}

		responseEntity.getBody();
	}

	/**
	 * Check whether a auth-backend is enabled.
	 *
	 * @param secretBackend
	 * @return
	 */
	public boolean hasSecret(String secretBackend) {

		Assert.hasText(secretBackend, "SecretBackend must not be empty");
		return hasMount(SYS_MOUNTS_URL_TEMPLATE, secretBackend);
	}

	private boolean hasMount(String urlTemplate, String type) {
		Map<String, String> parameters = parameters(vaultProperties);

		HttpEntity<Map<String, String>> entity = new HttpEntity<>(authenticatedHeaders());

		ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
				urlTemplate, HttpMethod.GET, entity, MAP_OF_MAPS_TYPE, parameters);

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Cannot enumerate mounts: " + responseEntity.toString());
		}

		Map<String, Object> body = responseEntity.getBody();
		for (Entry<String, Object> entry : body.entrySet()) {

			if (entry.getValue() instanceof Map) {
				Map<String, Object> nested = (Map<String, Object>) entry.getValue();

				if (entry.getKey().contains(type) && type.equals(nested.get("type"))) {
					return true;
				}
			}

		}

		return false;
	}

	/**
	 * Write key-value data to the Vault secret backend.
	 *
	 * @param path
	 * @param data
	 */
	public void writeSecret(String path, Map<String, ?> data) {

		Assert.hasText(path, "Path must not be empty");
		write("secret/" + path, data);
	}

	/**
	 * Write key-value data to a path in Vault.
	 *
	 * @param path
	 * @param data
	 */
	public void write(String path, Map<String, ?> data) {
		write(HttpMethod.POST, path, data);
	}

	/**
	 * Write key-value data to a path in Vault.
	 *
	 * @param httpMethod
	 * @param path
	 * @param data
	 */
	public void write(HttpMethod httpMethod, String path, Map<String, ?> data) {

		Assert.notNull(httpMethod, "HttpMethod must not be null");
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(data, "Data must not be null");

		HttpHeaders headers = authenticatedHeaders();

		Map<String, String> parameters = parameters(vaultProperties);
		parameters.put("path", path);

		ResponseEntity<String> exchange = restTemplate.exchange(WRITE_URL_TEMPLATE,
				HttpMethod.PUT, new HttpEntity<Object>(data, headers), String.class,
				parameters);

		if (!exchange.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					String.format("Cannot write to %s: %s", path, exchange.getBody()));
		}
	}

	/**
	 * Create an userId to appId mapping.
	 *
	 * @param appId
	 * @param userId
	 */
	public void mapUserId(String appId, String userId) {

		Map<String, String> userIdData = new HashMap<>();
		userIdData.put("value", appId); // name of the app-id
		userIdData.put("cidr_block", "0.0.0.0/0");

		String appIdPath = vaultProperties.getAppId().getAppIdPath();
		if (!hasAuth(appIdPath)) {
			mountAuth(appIdPath);
		}

		write(String.format("auth/%s/map/user-id/%s", appIdPath, userId), userIdData);
	}

	/**
	 * Create an appId mapping.
	 *
	 * @param appId
	 */
	public void mapAppId(String appId) {

		Map<String, String> appIdData = new HashMap<>();
		appIdData.put("value", "root"); // policy
		appIdData.put("display_name", "this is my test application");

		write(String.format("auth/%s/map/app-id/%s",
				vaultProperties.getAppId().getAppIdPath(), appId), appIdData);
	}

	private HttpHeaders authenticatedHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(VaultClient.VAULT_TOKEN, rootToken.getToken());
		return headers;
	}

	private Map<String, String> parameters(VaultProperties vaultProperties) {

		Map<String, String> parameters = new HashMap<>();

		String baseUri = String.format("%s://%s:%s/%s", vaultProperties.getScheme(),
				vaultProperties.getHost(), vaultProperties.getPort(),
				VaultClient.API_VERSION);
		parameters.put("baseuri", baseUri);

		return parameters;
	}

	/**
	 * @author Mark Paluch
	 */
	@Data
	static class TokenCreated {

		@JsonProperty("lease_duration")
		private long leaseDuration;
		@JsonProperty("renewable")
		private boolean renewable;
		@JsonProperty("auth")
		private AuthToken auth;

	}

	/**
	 * @author Mark Paluch
	 */
	@Value(staticConstructor = "of")
	static class InitializeVault {

		@JsonProperty("secret_shares")
		private int secretShares;

		@JsonProperty("secret_threshold")
		private int secretThreshold;
	}

	/**
	 * @author Mark Paluch
	 */
	@Data
	static class CreateToken {

		@JsonProperty("id")
		private String id;

		@JsonProperty("policies")
		private List<String> policies;

		@JsonProperty("ttl")
		private String ttl;
	}

	/**
	 * @author Mark Paluch
	 */
	@Value(staticConstructor = "of")
	static class UnsealKey {
		@JsonProperty
		@NonNull
		private String key;
	}

	/**
	 * @author Mark Paluch
	 */
	@Data
	static class UnsealProgress {

		@JsonProperty("sealed")
		private boolean sealed;
		@JsonProperty("t")
		private int t;
		@JsonProperty("n")
		private int n;
		@JsonProperty("progress")
		private int progress;
	}

	/**
	 * @author Mark Paluch
	 */
	@Data
	static class VaultInitialized {

		@JsonProperty("keys")
		private List<String> keys;
		@JsonProperty("root_token")
		private String rootToken;
	}

	/**
	 * @author Mark Paluch
	 */
	@Data
	public static class AuthToken {

		@JsonProperty("client_token")
		private String clientToken;

		@JsonProperty("policies")
		private List<String> policies;

		@JsonProperty("metadata")
		private Map<String, Object> metadata;

		@JsonProperty("lease_duration")
		private long leaseDuration;

		@JsonProperty("renewable")
		private boolean renewable;
	}
}
