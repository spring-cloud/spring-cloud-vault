/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.vault.config;

import java.net.URI;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.*;
import org.springframework.vault.authentication.AwsEc2AuthenticationOptions.Nonce;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestOperations;

/**
 * Factory for {@link ClientAuthentication}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
@RequiredArgsConstructor
class ClientAuthenticationFactory {

	private final VaultProperties vaultProperties;

	private final RestOperations restOperations;

	/**
	 * @return a new {@link ClientAuthentication}.
	 */
	public ClientAuthentication createClientAuthentication() {

		switch (vaultProperties.getAuthentication()) {

		case TOKEN:
			Assert.hasText(vaultProperties.getToken(),
					"Token (spring.cloud.vault.token) must not be empty");
			return new TokenAuthentication(vaultProperties.getToken());

		case APPID:
			return appIdAuthentication(vaultProperties);

		case APPROLE:
			return appRoleAuthentication(vaultProperties);

		case CERT:
			return new ClientCertificateAuthentication(restOperations);

		case AWS_EC2:
			return awsEc2Authentication(vaultProperties);

		case CUBBYHOLE:
			return cubbyholeAuthentication();
		}

		throw new UnsupportedOperationException(String.format(
				"Client authentication %s not supported",
				vaultProperties.getAuthentication()));
	}

	private ClientAuthentication appIdAuthentication(VaultProperties vaultProperties) {

		VaultProperties.AppIdProperties appId = vaultProperties.getAppId();
		Assert.hasText(appId.getUserId(),
				"UserId (spring.cloud.vault.app-id.user-id) must not be empty");

		AppIdAuthenticationOptions authenticationOptions = AppIdAuthenticationOptions
				.builder().appId(vaultProperties.getApplicationName()) //
				.path(appId.getAppIdPath()) //
				.userIdMechanism(getClientAuthentication(appId)).build();

		return new AppIdAuthentication(authenticationOptions, restOperations);
	}

	private AppIdUserIdMechanism getClientAuthentication(
			VaultProperties.AppIdProperties appId) {

		try {
			Class<?> userIdClass = ClassUtils.forName(appId.getUserId(), null);
			return (AppIdUserIdMechanism) BeanUtils.instantiateClass(userIdClass);
		}
		catch (ClassNotFoundException ex) {

			switch (appId.getUserId().toUpperCase()) {

			case VaultProperties.AppIdProperties.IP_ADDRESS:
				return new IpAddressUserId();

			case VaultProperties.AppIdProperties.MAC_ADDRESS:

				if (StringUtils.hasText(appId.getNetworkInterface())) {
					try {
						return new MacAddressUserId(Integer.parseInt(appId
								.getNetworkInterface()));
					}
					catch (NumberFormatException e) {
						return new MacAddressUserId(appId.getNetworkInterface());
					}
				}

				return new MacAddressUserId();
			default:
				return new StaticUserId(appId.getUserId());
			}
		}
	}

	private ClientAuthentication appRoleAuthentication(VaultProperties vaultProperties) {

		VaultProperties.AppRoleProperties appRole = vaultProperties.getAppRole();
		Assert.hasText(appRole.getRoleId(),
				"RoleId (spring.cloud.vault.app-role.role-id) must not be empty");

		AppRoleAuthenticationOptions.AppRoleAuthenticationOptionsBuilder builder = AppRoleAuthenticationOptions
				.builder().path(appRole.getAppRolePath()).roleId(appRole.getRoleId());

		if (StringUtils.hasText(appRole.getSecretId())) {
			builder = builder.secretId(appRole.getSecretId());
		}

		return new AppRoleAuthentication(builder.build(), restOperations);
	}

	private ClientAuthentication awsEc2Authentication(VaultProperties vaultProperties) {

		VaultProperties.AwsEc2Properties awsEc2 = vaultProperties.getAwsEc2();

		Nonce nonce = StringUtils.hasText(awsEc2.getNonce()) ? Nonce.provided(awsEc2
				.getNonce().toCharArray()) : Nonce.generated();

		AwsEc2AuthenticationOptions authenticationOptions = AwsEc2AuthenticationOptions
				.builder().role(awsEc2.getRole()) //
				.path(awsEc2.getAwsEc2Path()) //
				.nonce(nonce) //
				.identityDocumentUri(URI.create(awsEc2.getIdentityDocument())) //
				.build();

		return new AwsEc2Authentication(authenticationOptions, restOperations,
				restOperations);
	}

	private ClientAuthentication cubbyholeAuthentication() {

		Assert.hasText(vaultProperties.getToken(),
				"Initial Token (spring.cloud.vault.token) for Cubbyhole authentication must not be empty");

		CubbyholeAuthenticationOptions options = CubbyholeAuthenticationOptions.builder() //
				.wrapped() //
				.initialToken(VaultToken.of(vaultProperties.getToken())) //
				.build();

		return new CubbyholeAuthentication(options, restOperations);
	}
}
