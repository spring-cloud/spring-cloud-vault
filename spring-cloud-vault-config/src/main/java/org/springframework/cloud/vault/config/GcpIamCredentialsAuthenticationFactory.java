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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.cloud.vault.config.VaultProperties.GcpIamProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.GcpIamCredentialsAuthentication;
import org.springframework.vault.authentication.GcpIamCredentialsAuthenticationOptions;
import org.springframework.vault.authentication.GcpIamCredentialsAuthenticationOptions.GcpIamCredentialsAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.GoogleCredentialsSupplier;
import org.springframework.web.client.RestOperations;

/**
 * Utility to create {@link GcpIamCredentialsAuthentication} for the IAM Credentials
 * authentication method.
 *
 * @author Mark Paluch
 * @since 3.0.2
 */
final class GcpIamCredentialsAuthenticationFactory {

	private GcpIamCredentialsAuthenticationFactory() {
	}

	static ClientAuthentication create(VaultProperties vaultProperties, RestOperations restOperations) {

		GcpIamProperties gcp = vaultProperties.getGcpIam();

		Assert.hasText(gcp.getRole(), "Role (spring.cloud.vault.gcp-iam.role) must not be empty");

		GcpIamCredentialsAuthenticationOptionsBuilder builder = GcpIamCredentialsAuthenticationOptions.builder()
				.path(gcp.getGcpPath()).role(gcp.getRole()).jwtValidity(gcp.getJwtValidity());

		if (StringUtils.hasText(gcp.getServiceAccountId())) {
			builder.serviceAccountId(gcp.getServiceAccountId());
		}

		GoogleCredentialsSupplier supplier = () -> getGoogleCredential(gcp);
		builder.credentials(supplier.get());

		GcpIamCredentialsAuthenticationOptions options = builder.build();

		return new GcpIamCredentialsAuthentication(options, restOperations);
	}

	private static GoogleCredentials getGoogleCredential(GcpIamProperties gcp) throws IOException {

		VaultProperties.GcpCredentials credentialProperties = gcp.getCredentials();
		if (credentialProperties.getLocation() != null) {
			return GoogleCredentials.fromStream(credentialProperties.getLocation().getInputStream());
		}

		if (StringUtils.hasText(credentialProperties.getEncodedKey())) {
			return GoogleCredentials.fromStream(
					new ByteArrayInputStream(Base64.getDecoder().decode(credentialProperties.getEncodedKey())));
		}

		return GoogleCredentials.getApplicationDefault();
	}

}
