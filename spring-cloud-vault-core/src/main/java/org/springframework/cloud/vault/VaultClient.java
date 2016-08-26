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
package org.springframework.cloud.vault;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Vault client. This client reads data from Vault.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
public class VaultClient {

	public static final String API_VERSION = "v1";
	public static final String VAULT_TOKEN = "X-Vault-Token";

	@Setter
	@Getter
	private RestTemplate restTemplate;

	public VaultClient() {
		this(new RestTemplate());
	}

	public VaultClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Read data from the given Vault {@code uri} using the {@link VaultToken}.
	 *
	 * @param uri must not be {@literal null}.
	 * @param vaultToken must not be {@literal null}.
	 * @return A {@link Map} containing properties.
	 */
	public VaultClientResponse read(URI uri, VaultToken vaultToken) {

		Assert.notNull(uri, "URI must not be empty!");
		Assert.notNull(vaultToken, "Vault Token must not be null!");

		return exchange(uri, HttpMethod.GET, new HttpEntity<>(createHeaders(vaultToken)));
	}

	/**
	 * Write data to the given Vault {@code uri} using the {@link VaultToken}.
	 *
	 * @param uri must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @return A {@link Map} containing properties.
	 */
	public VaultClientResponse write(URI uri, Object entity) {

		Assert.notNull(uri, "URI must not be empty!");
		Assert.notNull(entity, "Entity must not be null!");

		return exchange(uri, HttpMethod.POST, new HttpEntity<>(entity));
	}

	/**
	 * Write data to the given Vault {@code uri} using the {@link VaultToken}.
	 *
	 * @param uri must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @param vaultToken must not be {@literal null}.
	 * @return A {@link Map} containing properties.
	 */
	public VaultClientResponse write(URI uri, Object entity, VaultToken vaultToken) {

		Assert.notNull(uri, "URI must not be empty!");
		Assert.notNull(entity, "Vault Token must not be null!");
		Assert.notNull(vaultToken, "Vault Token must not be null!");

		return exchange(uri, HttpMethod.POST, new HttpEntity<>(entity,
				createHeaders(vaultToken)));
	}

	/**
	 * Query the current Vault service for it's health status
	 *
	 * @param uri must not be {@literal null}.
	 * @return A {@link VaultHealthResponse} containing the current service status.
	 */
	public VaultHealthResponse health(URI uri) {
		try {
			ResponseEntity<VaultHealthResponse> healthResponse = this.restTemplate.exchange(
					uri, HttpMethod.GET, null,
					VaultHealthResponse.class);
			return healthResponse.getBody();
		} catch (HttpStatusCodeException responseError) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readValue(responseError.getResponseBodyAsString(), VaultHealthResponse.class);
			}
			catch (Exception jsonError) {
				throw responseError;
			}
		}
	}

	private VaultClientResponse exchange(URI uri, HttpMethod httpMethod,
			HttpEntity<?> httpEntity) {

		Assert.notNull(uri, "URI must not be empty!");

		try {
			ResponseEntity<VaultResponse> response = this.restTemplate.exchange(uri,
					httpMethod, httpEntity, VaultResponse.class);

			return VaultClientResponse.of(response.getBody(), response.getStatusCode(),
					uri, response.getStatusCode().getReasonPhrase());
		}
		catch (HttpServerErrorException | HttpClientErrorException e) {

			String message = e.getResponseBodyAsString();

			if (MediaType.APPLICATION_JSON.includes(e.getResponseHeaders()
					.getContentType())) {
				message = VaultErrorMessage.getError(message);
			}

			return VaultClientResponse.of(null, e.getStatusCode(), uri, message);
		}
	}

	/**
	 * Build the Vault {@link URI} based on the given {@link VaultProperties} and
	 * {@code path}.
	 *
	 * @param properties must not be {@literal null}.
	 * @param path must not be empty or {@literal null}.
	 * @return
	 */
	public static URI buildUri(VaultProperties properties, String path) {
		return URI.create(createBaseUrlWithPath(properties, path));
	}

	/**
	 * Build the Vault {@link URI} based on the given {@link VaultProperties} and
	 * {@code pathTemplate}. URI template variables will be expanded using
	 * {@code uriVariables}.
	 *
	 * @param properties must not be {@literal null}.
	 * @param pathTemplate must not be empty or {@literal null}.
	 * @param uriVariables must not be {@literal null}.
	 * @see org.springframework.web.util.UriComponentsBuilder
	 * @return
	 */
	public URI buildUri(VaultProperties properties, String pathTemplate,
			Map<String, ?> uriVariables) {

		Assert.notNull(properties, "VaultProperties must not be null!");
		Assert.hasText(pathTemplate, "Path must not be empty!");
		Assert.notNull(properties, "Vault Token must not be null!");

		return restTemplate.getUriTemplateHandler().expand(
				createBaseUrlWithPath(properties, pathTemplate), uriVariables);
	}

	private HttpHeaders createHeaders(VaultToken vaultToken) {

		Assert.notNull(vaultToken, "Vault Token must not be null!");

		HttpHeaders headers = new HttpHeaders();
		headers.add(VAULT_TOKEN, vaultToken.getToken());
		return headers;
	}

	private static String createBaseUrlWithPath(VaultProperties properties, String path) {

		Assert.notNull(properties, "VaultProperties must not be null!");
		Assert.hasText(path, "Path must not be empty!");

		return String.format("%s://%s:%s/%s/%s", properties.getScheme(),
				properties.getHost(), properties.getPort(), API_VERSION, path);
	}

}
