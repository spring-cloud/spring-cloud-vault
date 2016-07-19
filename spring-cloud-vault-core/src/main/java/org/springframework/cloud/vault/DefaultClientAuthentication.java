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

import static org.springframework.cloud.vault.VaultClient.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Default implementation of {@link ClientAuthentication}.
 *
 * @author Mark Paluch
 */
@CommonsLog
class DefaultClientAuthentication extends ClientAuthentication {

	private final VaultProperties properties;
	private final RestTemplate restTemplate;
	private final AppIdUserIdMechanism appIdUserIdMechanism;
	private char[] nonce;

	/**
	 * Creates a {@link DefaultClientAuthentication} using {@link VaultProperties} and
	 * {@link RestTemplate}.
	 *
	 * @param properties must not be {@literal null}.
	 * @param restTemplate must not be {@literal null}.
	 */
	DefaultClientAuthentication(VaultProperties properties, RestTemplate restTemplate) {

		Assert.notNull(properties, "VaultProperties must not be null");
		Assert.notNull(restTemplate, "RestTemplate must not be null");

		this.properties = properties;
		this.restTemplate = restTemplate;
		this.appIdUserIdMechanism = null;
	}

	/**
	 * Creates a {@link DefaultClientAuthentication} using {@link VaultProperties} and
	 * {@link RestTemplate} for AppId authentication.
	 *
	 * @param properties must not be {@literal null}.
	 * @param restTemplate must not be {@literal null}.
	 * @param appIdUserIdMechanism must not be {@literal null}.
	 */
	DefaultClientAuthentication(VaultProperties properties, RestTemplate restTemplate,
			AppIdUserIdMechanism appIdUserIdMechanism) {

		Assert.notNull(properties, "VaultProperties must not be null");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(appIdUserIdMechanism, "AppIdUserIdMechanism must not be null");

		this.properties = properties;
		this.restTemplate = restTemplate;
		this.appIdUserIdMechanism = appIdUserIdMechanism;
	}

	@Override
	public VaultToken login() {

		if (properties.getAuthentication() == VaultProperties.AuthenticationMethod.APPID
				&& appIdUserIdMechanism != null) {
			log.info("Using AppId authentication to log into Vault");

			VaultProperties.AppIdProperties appId = properties.getAppId();
			return createTokenUsingAppId(new AppIdTuple(properties.getApplicationName(),
					appIdUserIdMechanism.createUserId()), appId);
		}

		if (properties
				.getAuthentication() == VaultProperties.AuthenticationMethod.AWS_EC2) {
			log.info("Using AWS-EC2 authentication to log into Vault");

			return createTokenUsingAwsEc2();
		}

		throw new UnsupportedOperationException(
				String.format("Cannot create a token for auth method %s",
						properties.getAuthentication()));
	}

	private VaultToken createTokenUsingAppId(AppIdTuple appIdTuple,
			VaultProperties.AppIdProperties appId) {

		String url = buildUrl();
		Map<String, String> variables = new HashMap<>();
		variables.put("backend", "auth/" + appId.getAppIdPath());
		variables.put("key", "login");

		Map<String, String> login = getAppIdLogin(appIdTuple);

		try {
			ResponseEntity<VaultResponse> response = restTemplate.postForEntity(url,
					new HttpEntity<>(login), VaultResponse.class, variables);

			HttpStatus status = response.getStatusCode();
			if (!status.is2xxSuccessful()) {
				throw new IllegalStateException("Cannot login using app-id");
			}

			VaultResponse body = response.getBody();
			String token = (String) body.getAuth().get("client_token");

			log.debug("Login successful using AppId authentication");

			return VaultToken.of(token, body.getLeaseDuration());
		}
		catch (HttpStatusCodeException e) {

			if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
				throw new IllegalStateException(
						String.format("Cannot login using app-id: %s",
								VaultErrorMessage.getError(e.getResponseBodyAsString())));
			}
			if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
				throw new IllegalStateException(
						String.format("Cannot login using app-id: %s",
								VaultErrorMessage.getError(e.getResponseBodyAsString())));
			}

			throw e;
		}
	}

	private Map<String, String> getAppIdLogin(AppIdTuple appIdTuple) {

		Map<String, String> login = new HashMap<>();
		login.put("app_id", appIdTuple.getAppId());
		login.put("user_id", appIdTuple.getUserId());
		return login;
	}

	@SuppressWarnings("unchecked")
	private VaultToken createTokenUsingAwsEc2() {

		VaultProperties.AwsEc2Properties properties = this.properties.getAwsEc2();

		String url = buildUrl();
		Map<String, String> variables = new HashMap<>();
		variables.put("backend", "auth/" + properties.getAwsEc2Path());
		variables.put("key", "login");

		try {

			Map<String, String> login = getEc2Login(properties);

			ResponseEntity<VaultResponse> response = restTemplate.postForEntity(url,
					new HttpEntity<>(login), VaultResponse.class, variables);

			HttpStatus status = response.getStatusCode();
			if (!status.is2xxSuccessful()) {
				throw new IllegalStateException("Cannot login using AWS-EC2");
			}

			VaultResponse body = response.getBody();
			String token = (String) body.getAuth().get("client_token");

			if (log.isDebugEnabled()) {

				if (body.getAuth().get("metadata") instanceof Map) {
					Map<Object, Object> metadata = (Map<Object, Object>) body.getAuth()
							.get("metadata");
					log.debug(String.format(
							"Login successful using AWS-EC2 authentication for instance %s, AMI %s",
							metadata.get("instance_id"), metadata.get("instance_id")));
				}
				else {
					log.debug("Login successful using AWS-EC2 authentication");
				}
			}

			return VaultToken.of(token, body.getLeaseDuration());
		}
		catch (HttpStatusCodeException e) {

			if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
				throw new IllegalStateException(
						String.format("Cannot login using AWS-EC2: %s",
								VaultErrorMessage.getError(e.getResponseBodyAsString())));
			}

			throw e;
		}
	}

	private Map<String, String> getEc2Login(VaultProperties.AwsEc2Properties properties) {

		Map<String, String> login = new HashMap<>();

		if (StringUtils.hasText(properties.getRole())) {
			login.put("role", properties.getRole());
		}

		if (properties.isUseNonce()) {
			if (this.nonce == null) {
				this.nonce = createNonce();
			}

			login.put("nonce", new String(this.nonce));
		}

		String pkcs7 = restTemplate.getForObject(properties.getIdentityDocument(),
				String.class);
		if (StringUtils.hasText(pkcs7)) {
			login.put("pkcs7", pkcs7.replaceAll("\\r", "").replace("\\n", ""));
		}
		return login;
	}

	private char[] createNonce() {
		return UUID.randomUUID().toString().toCharArray();
	}

	@Value
	private static class AppIdTuple {
		private String appId;
		private String userId;
	}

	private String buildUrl() {
		return String.format("%s://%s:%s/%s/{backend}/{key}", this.properties.getScheme(),
				this.properties.getHost(), this.properties.getPort(), API_VERSION);
	}
}
