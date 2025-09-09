/*
 * Copyright 2020-present the original author or authors.
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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import org.springframework.cloud.vault.config.VaultProperties.GcpIamProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.GcpCredentialSupplier;
import org.springframework.vault.authentication.GcpIamAuthentication;
import org.springframework.vault.authentication.GcpIamAuthenticationOptions;
import org.springframework.vault.authentication.GcpIamAuthenticationOptions.GcpIamAuthenticationOptionsBuilder;
import org.springframework.web.client.RestOperations;

/**
 * Utility to create {@link GcpIamAuthentication} for the IAM authentication method.
 *
 * @author Mark Paluch
 * @since 3.0.2
 */
final class GcpIamAuthenticationFactory {

	private GcpIamAuthenticationFactory() {
	}

	static ClientAuthentication create(VaultProperties vaultProperties, RestOperations restOperations) {

		VaultProperties.GcpIamProperties gcp = vaultProperties.getGcpIam();

		Assert.hasText(gcp.getRole(), "Role (spring.cloud.vault.gcp-iam.role) must not be empty");

		GcpIamAuthenticationOptionsBuilder builder = GcpIamAuthenticationOptions.builder()
			.path(gcp.getGcpPath())
			.role(gcp.getRole())
			.jwtValidity(gcp.getJwtValidity());

		if (StringUtils.hasText(gcp.getProjectId())) {
			builder.projectId(gcp.getProjectId());
		}

		if (StringUtils.hasText(gcp.getServiceAccountId())) {
			builder.serviceAccountId(gcp.getServiceAccountId());
		}

		GcpCredentialSupplier supplier = () -> getGoogleCredential(gcp);
		builder.credential(supplier.get());

		GcpIamAuthenticationOptions options = builder.build();

		return new GcpIamAuthentication(options, restOperations);
	}

	private static GoogleCredential getGoogleCredential(GcpIamProperties gcp) throws IOException {

		VaultProperties.GcpCredentials credentialProperties = gcp.getCredentials();
		if (credentialProperties.getLocation() != null) {
			return GoogleCredential.fromStream(credentialProperties.getLocation().getInputStream());
		}

		if (StringUtils.hasText(credentialProperties.getEncodedKey())) {
			return GoogleCredential
				.fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(credentialProperties.getEncodedKey())));
		}

		return GoogleCredential.getApplicationDefault();
	}

}
