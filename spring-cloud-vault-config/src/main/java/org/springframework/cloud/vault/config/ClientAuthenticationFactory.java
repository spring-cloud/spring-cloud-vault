/*
 * Copyright 2017-2020 the original author or authors.
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
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.vault.config.VaultProperties.AppRoleProperties;
import org.springframework.cloud.vault.config.VaultProperties.AwsIamProperties;
import org.springframework.cloud.vault.config.VaultProperties.AzureMsiProperties;
import org.springframework.cloud.vault.config.VaultProperties.GcpCredentials;
import org.springframework.cloud.vault.config.VaultProperties.GcpIamProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.AppIdAuthentication;
import org.springframework.vault.authentication.AppIdAuthenticationOptions;
import org.springframework.vault.authentication.AppIdUserIdMechanism;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.AppRoleAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.authentication.AwsEc2Authentication;
import org.springframework.vault.authentication.AwsEc2AuthenticationOptions;
import org.springframework.vault.authentication.AwsEc2AuthenticationOptions.Nonce;
import org.springframework.vault.authentication.AwsIamAuthentication;
import org.springframework.vault.authentication.AwsIamAuthenticationOptions;
import org.springframework.vault.authentication.AwsIamAuthenticationOptions.AwsIamAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.AzureMsiAuthentication;
import org.springframework.vault.authentication.AzureMsiAuthenticationOptions;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ClientCertificateAuthentication;
import org.springframework.vault.authentication.CubbyholeAuthentication;
import org.springframework.vault.authentication.CubbyholeAuthenticationOptions;
import org.springframework.vault.authentication.GcpComputeAuthentication;
import org.springframework.vault.authentication.GcpComputeAuthenticationOptions;
import org.springframework.vault.authentication.GcpComputeAuthenticationOptions.GcpComputeAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.GcpCredentialSupplier;
import org.springframework.vault.authentication.GcpIamAuthentication;
import org.springframework.vault.authentication.GcpIamAuthenticationOptions;
import org.springframework.vault.authentication.GcpIamAuthenticationOptions.GcpIamAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.IpAddressUserId;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.KubernetesServiceAccountTokenFile;
import org.springframework.vault.authentication.MacAddressUserId;
import org.springframework.vault.authentication.PcfAuthentication;
import org.springframework.vault.authentication.PcfAuthenticationOptions;
import org.springframework.vault.authentication.ResourceCredentialSupplier;
import org.springframework.vault.authentication.StaticUserId;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestOperations;

/**
 * Factory for {@link ClientAuthentication}.
 *
 * @author Mark Paluch
 * @author Kevin Holditch
 * @author Michal Budzyn
 * @since 1.1
 */
class ClientAuthenticationFactory {

	private final VaultProperties vaultProperties;

	private final RestOperations restOperations;

	private final RestOperations externalRestOperations;

	ClientAuthenticationFactory(VaultProperties vaultProperties, RestOperations restOperations,
			RestOperations externalRestOperations) {
		this.vaultProperties = vaultProperties;
		this.restOperations = restOperations;
		this.externalRestOperations = externalRestOperations;
	}

	/**
	 * @return a new {@link ClientAuthentication}.
	 */
	ClientAuthentication createClientAuthentication() {

		switch (this.vaultProperties.getAuthentication()) {

		case APPID:
			return appIdAuthentication(this.vaultProperties);

		case APPROLE:
			return appRoleAuthentication(this.vaultProperties);

		case AWS_EC2:
			return awsEc2Authentication(this.vaultProperties);

		case AWS_IAM:
			return awsIamAuthentication(this.vaultProperties);

		case AZURE_MSI:
			return azureMsiAuthentication(this.vaultProperties);

		case CERT:
			return new ClientCertificateAuthentication(this.restOperations);

		case CUBBYHOLE:
			return cubbyholeAuthentication();

		case GCP_GCE:
			return gcpGceAuthentication(this.vaultProperties);

		case GCP_IAM:
			return gcpIamAuthentication(this.vaultProperties);

		case KUBERNETES:
			return kubernetesAuthentication(this.vaultProperties);

		case PCF:
			return pcfAuthentication(this.vaultProperties);

		case TOKEN:
			Assert.hasText(this.vaultProperties.getToken(), "Token (spring.cloud.vault.token) must not be empty");
			return new TokenAuthentication(this.vaultProperties.getToken());
		}

		throw new UnsupportedOperationException(
				String.format("Client authentication %s not supported", this.vaultProperties.getAuthentication()));
	}

	private ClientAuthentication appIdAuthentication(VaultProperties vaultProperties) {

		VaultProperties.AppIdProperties appId = vaultProperties.getAppId();
		Assert.hasText(appId.getUserId(), "UserId (spring.cloud.vault.app-id.user-id) must not be empty");

		AppIdAuthenticationOptions authenticationOptions = AppIdAuthenticationOptions.builder()
				.appId(vaultProperties.getApplicationName()) //
				.path(appId.getAppIdPath()) //
				.userIdMechanism(getAppIdMechanism(appId)).build();

		return new AppIdAuthentication(authenticationOptions, this.restOperations);
	}

	private AppIdUserIdMechanism getAppIdMechanism(VaultProperties.AppIdProperties appId) {

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
						return new MacAddressUserId(Integer.parseInt(appId.getNetworkInterface()));
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

		AppRoleAuthenticationOptions options = getAppRoleAuthenticationOptions(vaultProperties);

		return new AppRoleAuthentication(options, this.restOperations);
	}

	static AppRoleAuthenticationOptions getAppRoleAuthenticationOptions(VaultProperties vaultProperties) {

		AppRoleProperties appRole = vaultProperties.getAppRole();

		AppRoleAuthenticationOptionsBuilder builder = AppRoleAuthenticationOptions.builder()
				.path(appRole.getAppRolePath());

		if (StringUtils.hasText(appRole.getRole())) {
			builder.appRole(appRole.getRole());
		}

		RoleId roleId = getRoleId(vaultProperties, appRole);
		SecretId secretId = getSecretId(vaultProperties, appRole);

		builder.roleId(roleId).secretId(secretId);

		return builder.build();
	}

	private static RoleId getRoleId(VaultProperties vaultProperties, AppRoleProperties appRole) {

		if (StringUtils.hasText(appRole.getRoleId())) {
			return RoleId.provided(appRole.getRoleId());
		}

		if (StringUtils.hasText(vaultProperties.getToken()) && StringUtils.hasText(appRole.getRole())) {
			return RoleId.pull(VaultToken.of(vaultProperties.getToken()));
		}

		if (StringUtils.hasText(vaultProperties.getToken())) {
			return RoleId.wrapped(VaultToken.of(vaultProperties.getToken()));
		}

		throw new IllegalArgumentException(
				"Cannot configure RoleId. Any of role-id, initial token, or initial token and role name must be configured.");
	}

	private static SecretId getSecretId(VaultProperties vaultProperties, AppRoleProperties appRole) {

		if (StringUtils.hasText(appRole.getSecretId())) {
			return SecretId.provided(appRole.getSecretId());
		}

		if (StringUtils.hasText(vaultProperties.getToken()) && StringUtils.hasText(appRole.getRole())) {
			return SecretId.pull(VaultToken.of(vaultProperties.getToken()));
		}

		if (StringUtils.hasText(vaultProperties.getToken())) {
			return SecretId.wrapped(VaultToken.of(vaultProperties.getToken()));
		}

		return SecretId.absent();
	}

	private ClientAuthentication awsEc2Authentication(VaultProperties vaultProperties) {

		VaultProperties.AwsEc2Properties awsEc2 = vaultProperties.getAwsEc2();

		Nonce nonce = StringUtils.hasText(awsEc2.getNonce()) ? Nonce.provided(awsEc2.getNonce().toCharArray())
				: Nonce.generated();

		AwsEc2AuthenticationOptions authenticationOptions = AwsEc2AuthenticationOptions.builder().role(awsEc2.getRole()) //
				.path(awsEc2.getAwsEc2Path()) //
				.nonce(nonce) //
				.identityDocumentUri(URI.create(awsEc2.getIdentityDocument())) //
				.build();

		return new AwsEc2Authentication(authenticationOptions, this.restOperations, this.externalRestOperations);
	}

	private ClientAuthentication awsIamAuthentication(VaultProperties vaultProperties) {

		AwsIamProperties awsIam = vaultProperties.getAwsIam();

		AWSCredentialsProvider credentialsProvider = AwsCredentialProvider.getAwsCredentialsProvider();

		AwsIamAuthenticationOptionsBuilder builder = AwsIamAuthenticationOptions.builder();

		if (StringUtils.hasText(awsIam.getRole())) {
			builder.role(awsIam.getRole());
		}

		if (StringUtils.hasText(awsIam.getServerName())) {
			builder.serverName(awsIam.getServerName());
		}

		if (awsIam.getEndpointUri() != null) {
			builder.endpointUri(awsIam.getEndpointUri());
		}

		builder.path(awsIam.getAwsPath()) //
				.credentialsProvider(credentialsProvider);

		AwsIamAuthenticationOptions options = builder.credentialsProvider(credentialsProvider).build();

		return new AwsIamAuthentication(options, this.restOperations);
	}

	private ClientAuthentication azureMsiAuthentication(VaultProperties vaultProperties) {

		AzureMsiProperties azureMsi = vaultProperties.getAzureMsi();

		Assert.hasText(azureMsi.getRole(), "Azure role (spring.cloud.vault.azure-msi.role) must not be empty");

		AzureMsiAuthenticationOptions options = AzureMsiAuthenticationOptions.builder() //
				.role(azureMsi.getRole()).path(azureMsi.getAzurePath()) //
				.instanceMetadataUri(azureMsi.getMetadataService()) //
				.identityTokenServiceUri(azureMsi.getIdentityTokenService()) //
				.build();

		return new AzureMsiAuthentication(options, this.restOperations, this.externalRestOperations);
	}

	private ClientAuthentication cubbyholeAuthentication() {

		Assert.hasText(this.vaultProperties.getToken(),
				"Initial Token (spring.cloud.vault.token) for Cubbyhole authentication must not be empty");

		CubbyholeAuthenticationOptions options = CubbyholeAuthenticationOptions.builder() //
				.wrapped() //
				.initialToken(VaultToken.of(this.vaultProperties.getToken())) //
				.build();

		return new CubbyholeAuthentication(options, this.restOperations);
	}

	private ClientAuthentication gcpGceAuthentication(VaultProperties vaultProperties) {

		VaultProperties.GcpGceProperties gcp = vaultProperties.getGcpGce();

		Assert.hasText(gcp.getRole(), "Role (spring.cloud.vault.gcp-gce.role) must not be empty");

		GcpComputeAuthenticationOptionsBuilder builder = GcpComputeAuthenticationOptions.builder()
				.path(gcp.getGcpPath()).role(gcp.getRole());

		if (StringUtils.hasText(gcp.getServiceAccount())) {
			builder.serviceAccount(gcp.getServiceAccount());
		}

		return new GcpComputeAuthentication(builder.build(), this.restOperations, this.externalRestOperations);
	}

	private ClientAuthentication gcpIamAuthentication(VaultProperties vaultProperties) {

		VaultProperties.GcpIamProperties gcp = vaultProperties.getGcpIam();

		Assert.hasText(gcp.getRole(), "Role (spring.cloud.vault.gcp-iam.role) must not be empty");

		GcpIamAuthenticationOptionsBuilder builder = GcpIamAuthenticationOptions.builder().path(gcp.getGcpPath())
				.role(gcp.getRole()).jwtValidity(gcp.getJwtValidity());

		if (StringUtils.hasText(gcp.getProjectId())) {
			builder.projectId(gcp.getProjectId());
		}

		if (StringUtils.hasText(gcp.getServiceAccountId())) {
			builder.serviceAccountId(gcp.getServiceAccountId());
		}

		GcpCredentialSupplier supplier = () -> getGoogleCredential(gcp);
		builder.credential(supplier.get());

		GcpIamAuthenticationOptions options = builder.build();

		return new GcpIamAuthentication(options, this.restOperations);
	}

	private GoogleCredential getGoogleCredential(GcpIamProperties gcp) throws IOException {

		GcpCredentials credentialProperties = gcp.getCredentials();
		if (credentialProperties.getLocation() != null) {
			return GoogleCredential.fromStream(credentialProperties.getLocation().getInputStream());
		}

		if (StringUtils.hasText(credentialProperties.getEncodedKey())) {
			return GoogleCredential.fromStream(
					new ByteArrayInputStream(Base64.getDecoder().decode(credentialProperties.getEncodedKey())));
		}

		return GoogleCredential.getApplicationDefault();
	}

	private ClientAuthentication kubernetesAuthentication(VaultProperties vaultProperties) {

		VaultProperties.KubernetesProperties kubernetes = vaultProperties.getKubernetes();

		Assert.hasText(kubernetes.getRole(), "Role (spring.cloud.vault.kubernetes.role) must not be empty");
		Assert.hasText(kubernetes.getServiceAccountTokenFile(),
				"Service account token file (spring.cloud.vault.kubernetes.service-account-token-file) must not be empty");

		KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
				.path(kubernetes.getKubernetesPath()).role(kubernetes.getRole())
				.jwtSupplier(new KubernetesServiceAccountTokenFile(kubernetes.getServiceAccountTokenFile())).build();

		return new KubernetesAuthentication(options, this.restOperations);
	}

	private ClientAuthentication pcfAuthentication(VaultProperties vaultProperties) {

		VaultProperties.PcfProperties pcfProperties = vaultProperties.getPcf();

		Assert.isTrue(ClassUtils.isPresent("org.bouncycastle.crypto.signers.PSSSigner", getClass().getClassLoader()),
				"BouncyCastle (bcpkix-jdk15on) must be on the classpath");
		Assert.hasText(pcfProperties.getRole(), "Role (spring.cloud.vault.pcf.role) must not be empty");

		PcfAuthenticationOptions.PcfAuthenticationOptionsBuilder builder = PcfAuthenticationOptions.builder()
				.role(pcfProperties.getRole()).path(pcfProperties.getPcfPath());

		if (pcfProperties.getInstanceCertificate() != null) {
			builder.instanceCertificate(new ResourceCredentialSupplier(pcfProperties.getInstanceCertificate()));
		}

		if (pcfProperties.getInstanceKey() != null) {
			builder.instanceKey(new ResourceCredentialSupplier(pcfProperties.getInstanceKey()));
		}

		return new PcfAuthentication(builder.build(), this.restOperations);
	}

	private static class AwsCredentialProvider {

		private static AWSCredentialsProvider getAwsCredentialsProvider() {

			DefaultAWSCredentialsProviderChain backingCredentialsProvider = DefaultAWSCredentialsProviderChain
					.getInstance();

			// Eagerly fetch credentials preventing lag during the first, actual login.
			AWSCredentials firstAccess = backingCredentialsProvider.getCredentials();

			AtomicReference<AWSCredentials> once = new AtomicReference<>(firstAccess);

			return new AWSCredentialsProvider() {

				@Override
				public AWSCredentials getCredentials() {

					if (once.compareAndSet(firstAccess, null)) {
						return firstAccess;
					}

					return backingCredentialsProvider.getCredentials();
				}

				@Override
				public void refresh() {
					backingCredentialsProvider.refresh();
				}
			};
		}

	}

}
