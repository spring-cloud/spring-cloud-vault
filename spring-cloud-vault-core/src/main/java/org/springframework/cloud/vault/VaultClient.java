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

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Vault client. This client reads data from Vault secret backends and can authenticate
 * with Vault to obtain an access token.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@CommonsLog
public class VaultClient {

	public static final String API_VERSION = "v1";
	public static final String VAULT_TOKEN = "X-Vault-Token";

	@Setter
	private RestTemplate rest = new RestTemplate();

	private final VaultProperties properties;

	public VaultClient(VaultProperties properties) {

		Assert.notNull(properties, "VaultProperties must not be null");

		this.properties = properties;
	}

	/**
	 * Read secrets using the given {@link SecureBackendAccessor} and {@link VaultToken}.
	 *
	 * @param secureBackendAccessor must not be {@literal null}.
	 * @param vaultToken must not be {@literal null}.
	 * @return A {@link Map} containing properties.
	 */
	public Map<String, String> read(SecureBackendAccessor secureBackendAccessor,
			VaultToken vaultToken) {

		Assert.notNull(secureBackendAccessor, "SecureBackendAccessor must not be empty!");
		Assert.notNull(vaultToken, "Vault Token must not be null!");

		String url = buildUrl();

		HttpHeaders headers = createHeaders(vaultToken);
		Exception error = null;
		String errorBody = null;
		HttpStatus status = null;

		URI uri = this.rest.getUriTemplateHandler().expand(url,
				secureBackendAccessor.variables());
		log.info(String.format("Fetching config from server at: %s", uri));

		try {
			ResponseEntity<VaultResponse> response = this.rest.exchange(uri,
					HttpMethod.GET, new HttpEntity<>(headers), VaultResponse.class);

			status = response.getStatusCode();
			if (status == HttpStatus.OK) {
				if (response.getBody().getData() != null) {
					return secureBackendAccessor
							.transformProperties(response.getBody().getData());
				}
			}
		}
		catch (HttpServerErrorException | HttpClientErrorException e) {

			if (MediaType.APPLICATION_JSON
					.includes(e.getResponseHeaders().getContentType())) {
				errorBody = e.getResponseBodyAsString();
			}

			status = e.getStatusCode();
			error = e;
		}
		catch (Exception e) {
			error = e;
		}

		if (status == HttpStatus.NOT_FOUND) {
			log.info(String.format("Could not locate PropertySource: %s",
					"key not found"));
		}
		else if (status != null) {
			log.warn(String.format("Could not locate PropertySource: Status %d %s",
					status.value(), getErrorMessage(error, errorBody)));
		}
		else {
			log.warn(String.format("Could not locate PropertySource: %s",
					(getErrorMessage(error, errorBody))));
		}

		if (properties.isFailFast()) {
			throw new IllegalStateException(
					"Could not locate PropertySource and the fail fast property is set, failing",
					error);
		}

		return Collections.emptyMap();
	}

	private String getErrorMessage(Exception error, String errorBody) {
		return errorBody == null ? error == null ? "unknown reason" : error.getMessage()
				: VaultErrorMessage.getError(errorBody);
	}

	private HttpHeaders createHeaders(VaultToken vaultToken) {

		HttpHeaders headers = new HttpHeaders();
		headers.add(VAULT_TOKEN, vaultToken.getToken());
		return headers;
	}

	private String buildUrl() {
		return String.format("%s://%s:%s/%s/{backend}/{key}", this.properties.getScheme(),
				this.properties.getHost(), this.properties.getPort(), API_VERSION);
	}
}
