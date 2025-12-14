/*
 * Copyright 2017-present the original author or authors.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import org.springframework.boot.system.SystemProperties;
import org.springframework.cloud.vault.config.VaultProperties.AppRoleProperties;
import org.springframework.cloud.vault.config.VaultProperties.AwsIamProperties;
import org.springframework.cloud.vault.config.VaultProperties.AzureMsiProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
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
import org.springframework.vault.authentication.ClientCertificateAuthenticationOptions;
import org.springframework.vault.authentication.ClientCertificateAuthenticationOptions.ClientCertificateAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.CubbyholeAuthentication;
import org.springframework.vault.authentication.CubbyholeAuthenticationOptions;
import org.springframework.vault.authentication.GcpComputeAuthentication;
import org.springframework.vault.authentication.GcpComputeAuthenticationOptions;
import org.springframework.vault.authentication.GcpComputeAuthenticationOptions.GcpComputeAuthenticationOptionsBuilder;
import org.springframework.vault.authentication.GitHubAuthentication;
import org.springframework.vault.authentication.GitHubAuthenticationOptions;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.KubernetesServiceAccountTokenFile;
import org.springframework.vault.authentication.PcfAuthentication;
import org.springframework.vault.authentication.PcfAuthenticationOptions;
import org.springframework.vault.authentication.ResourceCredentialSupplier;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestClient;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Factory for {@link ClientAuthentication}.
 *
 * @author Mark Paluch
 * @author Kevin Holditch
 * @author Michal Budzyn
 * @author Quincy Conduff
 * @author Issam El-atif
 * @since 1.1
 */
class ClientAuthenticationFactory {

	private static final boolean googleCredentialsPresent = ClassUtils
		.isPresent("com.google.auth.oauth2.GoogleCredentials", ClientAuthenticationFactory.class.getClassLoader());

	private final VaultProperties vaultProperties;

	private final VaultClient vaultClient;

	private final RestClient restClient;

	ClientAuthenticationFactory(VaultProperties vaultProperties, VaultClient vaultClient, RestClient restClient) {
		this.vaultProperties = vaultProperties;
		this.vaultClient = vaultClient;
		this.restClient = restClient;
	}

	/**
	 * @return a new {@link ClientAuthentication}.
	 */
	ClientAuthentication createClientAuthentication() {

		return switch (this.vaultProperties.getAuthentication()) {
			case APPROLE -> appRoleAuthentication(this.vaultProperties);
			case AWS_EC2 -> awsEc2Authentication(this.vaultProperties);
			case AWS_IAM -> awsIamAuthentication(this.vaultProperties);
			case AZURE_MSI -> azureMsiAuthentication(this.vaultProperties);
			case CERT -> certificateAuthentication(this.vaultProperties);
			case CUBBYHOLE -> cubbyholeAuthentication();
			case GCP_GCE -> gcpGceAuthentication(this.vaultProperties);
			case GCP_IAM -> gcpIamAuthentication(this.vaultProperties);
			case GITHUB -> gitHubAuthentication(this.vaultProperties);
			case KUBERNETES -> kubernetesAuthentication(this.vaultProperties);
			case PCF -> pcfAuthentication(this.vaultProperties);
			case TOKEN -> tokenAuthentication(this.vaultProperties);
			default -> throw new UnsupportedOperationException(
					String.format("Client authentication %s not supported", this.vaultProperties.getAuthentication()));
		};

	}

	private ClientAuthentication appRoleAuthentication(VaultProperties vaultProperties) {

		AppRoleAuthenticationOptions options = getAppRoleAuthenticationOptions(vaultProperties);

		return new AppRoleAuthentication(options, this.vaultClient);
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

		AwsEc2AuthenticationOptions authenticationOptions = AwsEc2AuthenticationOptions.builder()
			.role(awsEc2.getRole()) //
			.path(awsEc2.getAwsEc2Path()) //
			.nonce(nonce) //
			.identityDocumentUri(awsEc2.getIdentityDocument()) //
			.build();

		return new AwsEc2Authentication(authenticationOptions, this.vaultClient, this.restClient);
	}

	ClientAuthentication awsIamAuthentication(VaultProperties vaultProperties) {

		AwsIamProperties awsIam = vaultProperties.getAwsIam();

		AwsIamAuthenticationOptionsBuilder builder = AwsIamAuthenticationOptions.builder();
		AwsCredentialsProvider credentialsProvider = AwsCredentialProvider.getAwsCredentialsProvider();

		if (StringUtils.hasText(awsIam.getRegion())) {
			builder.region(Region.of(awsIam.getRegion()));
		}

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

		return new AwsIamAuthentication(options, this.vaultClient);
	}

	private ClientAuthentication azureMsiAuthentication(VaultProperties vaultProperties) {

		AzureMsiProperties azureMsi = vaultProperties.getAzureMsi();

		Assert.hasText(azureMsi.getRole(), "Azure role (spring.cloud.vault.azure-msi.role) must not be empty");

		AzureMsiAuthenticationOptions options = AzureMsiAuthenticationOptions.builder() //
			.role(azureMsi.getRole())
			.path(azureMsi.getAzurePath()) //
			.instanceMetadataUri(azureMsi.getMetadataService()) //
			.identityTokenServiceUri(azureMsi.getIdentityTokenService()) //
			.build();

		return new AzureMsiAuthentication(options, this.vaultClient, this.restClient);
	}

	private ClientAuthentication cubbyholeAuthentication() {

		Assert.hasText(this.vaultProperties.getToken(),
				"Initial Token (spring.cloud.vault.token) for Cubbyhole authentication must not be empty");

		CubbyholeAuthenticationOptions options = CubbyholeAuthenticationOptions.builder() //
			.wrapped() //
			.initialToken(VaultToken.of(this.vaultProperties.getToken())) //
			.build();

		return new CubbyholeAuthentication(options, this.vaultClient);
	}

	private ClientAuthentication gcpGceAuthentication(VaultProperties vaultProperties) {

		VaultProperties.GcpGceProperties gcp = vaultProperties.getGcpGce();

		Assert.hasText(gcp.getRole(), "Role (spring.cloud.vault.gcp-gce.role) must not be empty");

		GcpComputeAuthenticationOptionsBuilder builder = GcpComputeAuthenticationOptions.builder()
			.path(gcp.getGcpPath())
			.role(gcp.getRole());

		if (StringUtils.hasText(gcp.getServiceAccount())) {
			builder.serviceAccount(gcp.getServiceAccount());
		}

		return new GcpComputeAuthentication(builder.build(), this.vaultClient, this.restClient);
	}

	private ClientAuthentication gcpIamAuthentication(VaultProperties vaultProperties) {

		if (googleCredentialsPresent) {
			return GcpIamCredentialsAuthenticationFactory.create(vaultProperties, this.vaultClient);
		}

		throw new IllegalStateException(
				"Cannot create authentication mechanism for GCP IAM. This method requires the google-auth-library-oauth2-http dependency.");
	}

	private ClientAuthentication gitHubAuthentication(VaultProperties vaultProperties) {

		GitHubAuthenticationOptions options = GitHubAuthenticationOptions.builder()
			.path(vaultProperties.getGithub().getGithubPath())
			.token(getGitHubToken(vaultProperties))
			.build();

		return new GitHubAuthentication(options, this.vaultClient);
	}

	private String getGitHubToken(VaultProperties vaultProperties) {

		VaultProperties.GithubProperties github = vaultProperties.getGithub();
		String token = github.getToken();
		if (StringUtils.hasText(token)) {
			return token;
		}

		// Try to get token from gh cli if installed
		if (github.isAllowCliToken()) {
			return invokeGitHubCli("auth", "token");
		}

		throw new IllegalStateException("GitHub Token (spring.cloud.vault.github.token) must not be empty");
	}

	private static String invokeGitHubCli(String command, String subcommand) {

		ProcessBuilder processBuilder = new ProcessBuilder("gh", command, subcommand);

		try {
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				String error = new String(process.getErrorStream().readAllBytes());
				throw new IllegalStateException(String.format("""
						Cannot not retrieve GitHub token using GitHub CLI
						\tCommand: %s (Exit code %d)
						\tError:   %s""", StringUtils.collectionToDelimitedString(processBuilder.command(), " "),
						exitCode, error));
			}
			return new String(process.getInputStream().readAllBytes()).strip();
		}
		catch (InterruptedException | IOException ex) {
			throw new IllegalStateException(String.format("""
					Cannot not retrieve GitHub token using GitHub CLI
					\tCommand: %s""", StringUtils.collectionToDelimitedString(processBuilder.command(), " ")), ex);
		}
	}

	private ClientAuthentication kubernetesAuthentication(VaultProperties vaultProperties) {

		VaultProperties.KubernetesProperties kubernetes = vaultProperties.getKubernetes();

		Assert.hasText(kubernetes.getRole(), "Role (spring.cloud.vault.kubernetes.role) must not be empty");
		Assert.hasText(kubernetes.getServiceAccountTokenFile(),
				"Service account token file (spring.cloud.vault.kubernetes.service-account-token-file) must not be empty");

		KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
			.path(kubernetes.getKubernetesPath())
			.role(kubernetes.getRole())
			.jwtSupplier(new KubernetesServiceAccountTokenFile(kubernetes.getServiceAccountTokenFile()))
			.build();

		return new KubernetesAuthentication(options, this.vaultClient);
	}

	private ClientAuthentication pcfAuthentication(VaultProperties vaultProperties) {

		VaultProperties.PcfProperties pcfProperties = vaultProperties.getPcf();

		Assert.isTrue(ClassUtils.isPresent("org.bouncycastle.crypto.signers.PSSSigner", getClass().getClassLoader()),
				"BouncyCastle (bcpkix-jdk15on) must be on the classpath");
		Assert.hasText(pcfProperties.getRole(), "Role (spring.cloud.vault.pcf.role) must not be empty");

		PcfAuthenticationOptions.PcfAuthenticationOptionsBuilder builder = PcfAuthenticationOptions.builder()
			.role(pcfProperties.getRole())
			.path(pcfProperties.getPcfPath());

		if (pcfProperties.getInstanceCertificate() != null) {
			builder.instanceCertificate(new ResourceCredentialSupplier(pcfProperties.getInstanceCertificate()));
		}

		if (pcfProperties.getInstanceKey() != null) {
			builder.instanceKey(new ResourceCredentialSupplier(pcfProperties.getInstanceKey()));
		}

		return new PcfAuthentication(builder.build(), this.vaultClient);
	}

	private ClientAuthentication certificateAuthentication(VaultProperties vaultProperties) {

		ClientCertificateAuthenticationOptionsBuilder builder = ClientCertificateAuthenticationOptions.builder();
		builder.path(vaultProperties.getSsl().getCertAuthPath());

		if (StringUtils.hasText(vaultProperties.getSsl().getRole())) {
			builder.role(vaultProperties.getSsl().getRole());
		}

		return new ClientCertificateAuthentication(builder.build(), this.vaultClient);
	}

	private ClientAuthentication tokenAuthentication(VaultProperties vaultProperties) {

		if (StringUtils.hasText(vaultProperties.getToken())) {
			return new TokenAuthentication(vaultProperties.getToken());
		}

		Path vaultTokenPath = Paths.get(SystemProperties.get("user.home"), ".vault-token");

		if (Files.exists(vaultTokenPath)) {
			try {
				return new TokenAuthentication(new String(Files.readAllBytes(vaultTokenPath), UTF_8));
			}
			catch (IOException ex) {
				throw new IllegalStateException(String.format("Could not retrieve vault token from %s", vaultTokenPath),
						ex);
			}
		}
		else {
			throw new IllegalStateException(
					"Cannot create authentication mechanism for TOKEN. This method requires either a Token (spring.cloud.vault.token) or a token file at ~/.vault-token.");
		}
	}

	private static final class AwsCredentialProvider {

		private static AwsCredentialsProvider getAwsCredentialsProvider() {

			DefaultCredentialsProvider backingCredentialsProvider = DefaultCredentialsProvider.create();

			// Eagerly fetch credentials preventing lag during the first, actual login.
			AwsCredentials firstAccess = backingCredentialsProvider.resolveCredentials();

			AtomicReference<AwsCredentials> once = new AtomicReference<>(firstAccess);

			return new AwsCredentialsProvider() {

				@Override
				public AwsCredentials resolveCredentials() {

					if (once.compareAndSet(firstAccess, null)) {
						return firstAccess;
					}

					return backingCredentialsProvider.resolveCredentials();
				}

			};
		}

	}

}
