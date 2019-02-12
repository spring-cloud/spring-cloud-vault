/*
 * Copyright 2016-2018 the original author or authors.
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

import java.time.Duration;

import javax.validation.constraints.NotEmpty;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Kevin Holditch
 * @author Michal Budzyn
 */
@ConfigurationProperties("spring.cloud.vault")
@Data
@Validated
public class VaultProperties implements EnvironmentAware {

	/**
	 * Enable Vault config server.
	 */
	private boolean enabled = true;

	/**
	 * Vault server host.
	 */
	@NotEmpty
	private String host = "localhost";

	/**
	 * Vault server port.
	 */
	@Range(min = 1, max = 65535)
	private int port = 8200;

	/**
	 * Protocol scheme. Can be either "http" or "https".
	 */
	private String scheme = "https";

	/**
	 * Vault URI. Can be set with scheme, host and port.
	 */
	private String uri;

	/**
	 * Discovery properties.
	 */
	private Discovery discovery = new Discovery();

	/**
	 * Connection timeout.
	 */
	private int connectionTimeout = 5000;

	/**
	 * Read timeout.
	 */
	private int readTimeout = 15000;

	/**
	 * Fail fast if data cannot be obtained from Vault.
	 */
	private boolean failFast = false;

	/**
	 * Static vault token. Required if {@link #authentication} is {@code TOKEN}.
	 */
	private String token;

	private AppIdProperties appId = new AppIdProperties();

	private AppRoleProperties appRole = new AppRoleProperties();

	private AwsEc2Properties awsEc2 = new AwsEc2Properties();

	private AwsIamProperties awsIam = new AwsIamProperties();

	private AzureMsiProperties azureMsi = new AzureMsiProperties();

	private GcpGceProperties gcpGce = new GcpGceProperties();

	private GcpIamProperties gcpIam = new GcpIamProperties();

	private KubernetesProperties kubernetes = new KubernetesProperties();

	private Ssl ssl = new Ssl();

	private Config config = new Config();

	/**
	 * Application name for AppId authentication.
	 */
	private String applicationName = "application";

	private AuthenticationMethod authentication = AuthenticationMethod.TOKEN;

	@Override
	public void setEnvironment(Environment environment) {

		String springAppName = environment.getProperty("spring.application.name");

		if (StringUtils.hasText(springAppName)) {
			this.applicationName = springAppName;
		}
	}

	/**
	 * Enumeration of authentication methods.
	 */
	public enum AuthenticationMethod {

		TOKEN, APPID, APPROLE, AWS_EC2, AWS_IAM, AZURE_MSI, CERT, CUBBYHOLE, GCP_GCE, GCP_IAM, KUBERNETES

	}

	/**
	 * Discovery properties.
	 */
	@Data
	public static class Discovery {

		/**
		 * Default service Id.
		 */
		public static final String DEFAULT_VAULT = "vault";

		/**
		 * Flag to indicate that Vault server discovery is enabled (vault server URL will
		 * be looked up via discovery).
		 */
		private boolean enabled;

		/**
		 * Service id to locate Vault.
		 */
		private String serviceId = DEFAULT_VAULT;

	}

	/**
	 * AppId properties.
	 */
	@Data
	@Validated
	public static class AppIdProperties {

		/**
		 * Property value for UserId generation using a Mac-Address.
		 *
		 * @see org.springframework.vault.authentication.MacAddressUserId
		 */
		public static final String MAC_ADDRESS = "MAC_ADDRESS";

		/**
		 * Property value for UserId generation using an IP-Address.
		 *
		 * @see org.springframework.vault.authentication.IpAddressUserId
		 */
		public static final String IP_ADDRESS = "IP_ADDRESS";

		/**
		 * Mount path of the AppId authentication backend.
		 */
		private String appIdPath = "app-id";

		/**
		 * Network interface hint for the "MAC_ADDRESS" UserId mechanism.
		 */
		private String networkInterface = null;

		/**
		 * UserId mechanism. Can be either "MAC_ADDRESS", "IP_ADDRESS", a string or a
		 * class name.
		 */
		@NotEmpty
		private String userId = MAC_ADDRESS;

	}

	/**
	 * AppRole properties.
	 */
	@Data
	@Validated
	public static class AppRoleProperties {

		/**
		 * Mount path of the AppRole authentication backend.
		 */
		private String appRolePath = "approle";

		/**
		 * Name of the role, optional, used for pull-mode.
		 */
		private String role = "";

		/**
		 * The RoleId.
		 */
		private String roleId = null;

		/**
		 * The SecretId.
		 */
		private String secretId = null;

	}

	/**
	 * AWS-EC2 properties.
	 */
	@Data
	@Validated
	public static class AwsEc2Properties {

		/**
		 * URL of the AWS-EC2 PKCS7 identity document.
		 */
		@NotEmpty
		private String identityDocument = "http://169.254.169.254/latest/dynamic/instance-identity/pkcs7";

		/**
		 * Mount path of the AWS-EC2 authentication backend.
		 */
		@NotEmpty
		private String awsEc2Path = "aws-ec2";

		/**
		 * Name of the role, optional.
		 */
		private String role = "";

		/**
		 * Nonce used for AWS-EC2 authentication. An empty nonce defaults to nonce
		 * generation.
		 */
		private String nonce;

	}

	/**
	 * AWS-IAM properties.
	 */
	@Data
	public static class AwsIamProperties {

		/**
		 * Mount path of the AWS authentication backend.
		 */
		@NotEmpty
		private String awsPath = "aws";

		/**
		 * Name of the role, optional. Defaults to the friendly IAM name if not set.
		 */
		private String role = "";

		/**
		 * Name of the server used to set {@code X-Vault-AWS-IAM-Server-ID} header in the
		 * headers of login requests.
		 */
		private String serverName;

	}

	/**
	 * Azure MSI properties.
	 */
	@Data
	public static class AzureMsiProperties {

		/**
		 * Mount path of the Azure MSI authentication backend.
		 */
		@NotEmpty
		private String azurePath = "azure";

		/**
		 * Name of the role.
		 */
		private String role = "";

	}

	/**
	 * GCP-GCE properties.
	 */
	@Data
	public static class GcpGceProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		@NotEmpty
		private String gcpPath = "gcp";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Optional service account id. Using the default id if left unconfigured.
		 */
		private String serviceAccount = "";

	}

	/**
	 * GCP-IAM properties.
	 */
	@Data
	public static class GcpIamProperties {

		/**
		 * Credentials configuration.
		 */
		private final GcpCredentials credentials = new GcpCredentials();

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		@NotEmpty
		private String gcpPath = "gcp";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Overrides the GCP project Id.
		 */
		private String projectId = "";

		/**
		 * Overrides the GCP service account Id.
		 */
		private String serviceAccountId = "";

		/**
		 * Validity of the JWT token.
		 */
		private Duration jwtValidity = Duration.ofMinutes(15);

	}

	/**
	 * GCP credential properties.
	 */
	@Data
	public static class GcpCredentials {

		/**
		 * Location of the OAuth2 credentials private key.
		 *
		 * <p>
		 * Since this is a Resource, the private key can be in a multitude of locations,
		 * such as a local file system, classpath, URL, etc.
		 */
		private Resource location;

		/**
		 * The base64 encoded contents of an OAuth2 account private key in JSON format.
		 */
		private String encodedKey;

	}

	/**
	 * Kubernetes properties.
	 */
	@Data
	public static class KubernetesProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		@NotEmpty
		private String kubernetesPath = "kubernetes";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Path to the service account token file.
		 */
		@NotEmpty
		private String serviceAccountTokenFile = "/var/run/secrets/kubernetes.io/serviceaccount/token";

	}

	/**
	 * SSL properties.
	 */
	@Data
	@Validated
	public static class Ssl {

		/**
		 * Trust store that holds certificates and private keys.
		 */
		private Resource keyStore;

		/**
		 * Password used to access the key store.
		 */
		private String keyStorePassword;

		/**
		 * Trust store that holds SSL certificates.
		 */
		private Resource trustStore;

		/**
		 * Password used to access the trust store.
		 */
		private String trustStorePassword;

		/**
		 * Mount path of the TLS cert authentication backend.
		 */
		@NotEmpty
		private String certAuthPath = "cert";

	}

	/**
	 * Property source properties.
	 */
	@Data
	public static class Config {

		/**
		 * Used to set a {@link org.springframework.core.env.PropertySource} priority.
		 * This is useful to use Vault as an override on other property sources.
		 *
		 * @see org.springframework.core.PriorityOrdered
		 */
		private int order = 0;

		private Lifecycle lifecycle = new Lifecycle();

	}

	/**
	 * Configuration to Vault lifecycle management (renewal, revocation of tokens and
	 * secrets).
	 */
	@Data
	public static class Lifecycle {

		/**
		 * Enable lifecycle management.
		 */
		private boolean enabled = true;

		/**
		 * The amount of seconds that is at least required before renewing a lease.
		 */
		private Duration minRenewal;

		/**
		 * The expiry threshold. {@link Lease} is renewed the given seconds before it expires.
		 */
		private Duration expiryThreshold;

	}

}
