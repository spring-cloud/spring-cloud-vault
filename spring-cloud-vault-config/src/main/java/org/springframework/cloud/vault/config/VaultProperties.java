/*
 * Copyright 2016-2019 the original author or authors.
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

import java.net.URI;
import java.time.Duration;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.vault.core.lease.LeaseEndpoints;

/**
 * Properties to configure Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Kevin Holditch
 * @author Michal Budzyn
 * @author Grenville Wilson
 * @author Mårten Svantesson
 */
@ConfigurationProperties("spring.cloud.vault")
@Validated
public class VaultProperties implements EnvironmentAware {

	/**
	 * Enable Vault config server.
	 */
	private boolean enabled = true;

	/**
	 * Vault server host.
	 */
	// @NotEmpty
	private String host = "localhost";

	/**
	 * Vault server port.
	 */
	// @Range(min = 1, max = 65535)
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

	private PcfProperties pcf = new PcfProperties();

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

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getScheme() {
		return this.scheme;
	}

	public String getUri() {
		return this.uri;
	}

	public Discovery getDiscovery() {
		return this.discovery;
	}

	public int getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public int getReadTimeout() {
		return this.readTimeout;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public String getToken() {
		return this.token;
	}

	public AppIdProperties getAppId() {
		return this.appId;
	}

	public AppRoleProperties getAppRole() {
		return this.appRole;
	}

	public AwsEc2Properties getAwsEc2() {
		return this.awsEc2;
	}

	public AwsIamProperties getAwsIam() {
		return this.awsIam;
	}

	public AzureMsiProperties getAzureMsi() {
		return this.azureMsi;
	}

	public GcpGceProperties getGcpGce() {
		return this.gcpGce;
	}

	public GcpIamProperties getGcpIam() {
		return this.gcpIam;
	}

	public KubernetesProperties getKubernetes() {
		return this.kubernetes;
	}

	public PcfProperties getPcf() {
		return this.pcf;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Config getConfig() {
		return this.config;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public AuthenticationMethod getAuthentication() {
		return this.authentication;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setDiscovery(Discovery discovery) {
		this.discovery = discovery;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setAppId(AppIdProperties appId) {
		this.appId = appId;
	}

	public void setAppRole(AppRoleProperties appRole) {
		this.appRole = appRole;
	}

	public void setAwsEc2(AwsEc2Properties awsEc2) {
		this.awsEc2 = awsEc2;
	}

	public void setAwsIam(AwsIamProperties awsIam) {
		this.awsIam = awsIam;
	}

	public void setAzureMsi(AzureMsiProperties azureMsi) {
		this.azureMsi = azureMsi;
	}

	public void setGcpGce(GcpGceProperties gcpGce) {
		this.gcpGce = gcpGce;
	}

	public void setGcpIam(GcpIamProperties gcpIam) {
		this.gcpIam = gcpIam;
	}

	public void setKubernetes(KubernetesProperties kubernetes) {
		this.kubernetes = kubernetes;
	}

	public void setPcf(PcfProperties pcf) {
		this.pcf = pcf;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setAuthentication(AuthenticationMethod authentication) {
		this.authentication = authentication;
	}

	/**
	 * Enumeration of authentication methods.
	 */
	public enum AuthenticationMethod {

		APPID, APPROLE, AWS_EC2, AWS_IAM, AZURE_MSI, CERT, CUBBYHOLE, GCP_GCE, GCP_IAM, KUBERNETES, PCF, TOKEN;

	}

	/**
	 * Discovery properties.
	 */
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

		public boolean isEnabled() {
			return this.enabled;
		}

		public String getServiceId() {
			return this.serviceId;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

	}

	/**
	 * AppId properties.
	 */
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

		public String getAppIdPath() {
			return this.appIdPath;
		}

		public String getNetworkInterface() {
			return this.networkInterface;
		}

		public String getUserId() {
			return this.userId;
		}

		public void setAppIdPath(String appIdPath) {
			this.appIdPath = appIdPath;
		}

		public void setNetworkInterface(String networkInterface) {
			this.networkInterface = networkInterface;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

	}

	/**
	 * AppRole properties.
	 */
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

		public String getAppRolePath() {
			return this.appRolePath;
		}

		public String getRole() {
			return this.role;
		}

		public String getRoleId() {
			return this.roleId;
		}

		public String getSecretId() {
			return this.secretId;
		}

		public void setAppRolePath(String appRolePath) {
			this.appRolePath = appRolePath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setRoleId(String roleId) {
			this.roleId = roleId;
		}

		public void setSecretId(String secretId) {
			this.secretId = secretId;
		}

	}

	/**
	 * AWS-EC2 properties.
	 */
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

		public String getIdentityDocument() {
			return this.identityDocument;
		}

		public String getAwsEc2Path() {
			return this.awsEc2Path;
		}

		public String getRole() {
			return this.role;
		}

		public String getNonce() {
			return this.nonce;
		}

		public void setIdentityDocument(String identityDocument) {
			this.identityDocument = identityDocument;
		}

		public void setAwsEc2Path(String awsEc2Path) {
			this.awsEc2Path = awsEc2Path;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setNonce(String nonce) {
			this.nonce = nonce;
		}

	}

	/**
	 * AWS-IAM properties.
	 */
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

		/**
		 * STS server URI.
		 *
		 * @since 2.2
		 */
		private URI endpointUri;

		public String getAwsPath() {
			return this.awsPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getServerName() {
			return this.serverName;
		}

		public void setAwsPath(String awsPath) {
			this.awsPath = awsPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServerName(String serverName) {
			this.serverName = serverName;
		}

		public URI getEndpointUri() {
			return this.endpointUri;
		}

		public void setEndpointUri(URI endpointUri) {
			this.endpointUri = endpointUri;
		}

	}

	/**
	 * Azure MSI properties.
	 */
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

		public String getAzurePath() {
			return this.azurePath;
		}

		public String getRole() {
			return this.role;
		}

		public void setAzurePath(String azurePath) {
			this.azurePath = azurePath;
		}

		public void setRole(String role) {
			this.role = role;
		}

	}

	/**
	 * GCP-GCE properties.
	 */
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

		public String getGcpPath() {
			return this.gcpPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getServiceAccount() {
			return this.serviceAccount;
		}

		public void setGcpPath(String gcpPath) {
			this.gcpPath = gcpPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServiceAccount(String serviceAccount) {
			this.serviceAccount = serviceAccount;
		}

	}

	/**
	 * GCP-IAM properties.
	 */
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

		public GcpCredentials getCredentials() {
			return this.credentials;
		}

		public String getGcpPath() {
			return this.gcpPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getProjectId() {
			return this.projectId;
		}

		public String getServiceAccountId() {
			return this.serviceAccountId;
		}

		public Duration getJwtValidity() {
			return this.jwtValidity;
		}

		public void setGcpPath(String gcpPath) {
			this.gcpPath = gcpPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setProjectId(String projectId) {
			this.projectId = projectId;
		}

		public void setServiceAccountId(String serviceAccountId) {
			this.serviceAccountId = serviceAccountId;
		}

		public void setJwtValidity(Duration jwtValidity) {
			this.jwtValidity = jwtValidity;
		}

	}

	/**
	 * GCP credential properties.
	 */
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

		public Resource getLocation() {
			return this.location;
		}

		public String getEncodedKey() {
			return this.encodedKey;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

		public void setEncodedKey(String encodedKey) {
			this.encodedKey = encodedKey;
		}

	}

	/**
	 * Kubernetes properties.
	 */
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

		public String getKubernetesPath() {
			return this.kubernetesPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getServiceAccountTokenFile() {
			return this.serviceAccountTokenFile;
		}

		public void setKubernetesPath(String kubernetesPath) {
			this.kubernetesPath = kubernetesPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServiceAccountTokenFile(String serviceAccountTokenFile) {
			this.serviceAccountTokenFile = serviceAccountTokenFile;
		}

	}

	/**
	 * PCF properties.
	 */
	public static class PcfProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		@NotEmpty
		private String pcfPath = "pcf";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Path to the instance certificate (PEM). Defaults to {@code CF_INSTANCE_CERT}
		 * env variable.
		 */
		private Resource instanceCertificate;

		/**
		 * Path to the instance key (PEM). Defaults to {@code CF_INSTANCE_KEY} env
		 * variable.
		 */
		private Resource instanceKey;

		public String getPcfPath() {
			return this.pcfPath;
		}

		public void setPcfPath(String pcfPath) {
			this.pcfPath = pcfPath;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public Resource getInstanceCertificate() {
			return this.instanceCertificate;
		}

		public void setInstanceCertificate(Resource instanceCertificate) {
			this.instanceCertificate = instanceCertificate;
		}

		public Resource getInstanceKey() {
			return this.instanceKey;
		}

		public void setInstanceKey(Resource instanceKey) {
			this.instanceKey = instanceKey;
		}

	}

	/**
	 * SSL properties.
	 */
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

		public Resource getKeyStore() {
			return this.keyStore;
		}

		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public Resource getTrustStore() {
			return this.trustStore;
		}

		public String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		public String getCertAuthPath() {
			return this.certAuthPath;
		}

		public void setKeyStore(Resource keyStore) {
			this.keyStore = keyStore;
		}

		public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		public void setTrustStore(Resource trustStore) {
			this.trustStore = trustStore;
		}

		public void setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		public void setCertAuthPath(String certAuthPath) {
			this.certAuthPath = certAuthPath;
		}

	}

	/**
	 * Property source properties.
	 */
	public static class Config {

		/**
		 * Used to set a {@link org.springframework.core.env.PropertySource} priority.
		 * This is useful to use Vault as an override on other property sources.
		 *
		 * @see org.springframework.core.PriorityOrdered
		 */
		private int order = 0;

		private Lifecycle lifecycle = new Lifecycle();

		public int getOrder() {
			return this.order;
		}

		public Lifecycle getLifecycle() {
			return this.lifecycle;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		public void setLifecycle(Lifecycle lifecycle) {
			this.lifecycle = lifecycle;
		}

	}

	/**
	 * Configuration to Vault lifecycle management (renewal, revocation of tokens and
	 * secrets).
	 */
	public static class Lifecycle {

		/**
		 * Enable lifecycle management.
		 */
		private boolean enabled = true;

		/**
		 * The time period that is at least required before renewing a lease.
		 *
		 * @since 2.2
		 */
		private Duration minRenewal;

		/**
		 * The expiry threshold. {@link Lease} is renewed the given {@link Duration}
		 * before it expires.
		 *
		 * @since 2.2
		 */
		private Duration expiryThreshold;

		/**
		 * Set the {@link LeaseEndpoints} to delegate renewal/revocation calls to.
		 * {@link LeaseEndpoints} encapsulates differences between Vault versions that
		 * affect the location of renewal/revocation endpoints.
		 *
		 * Can be {@link LeaseEndpoints#SysLeases} for version 0.8 or above of Vault or
		 * {@link LeaseEndpoints#Legacy} for older versions (the default).
		 *
		 * @since 2.2
		 */
		private LeaseEndpoints leaseEndpoints;

		public boolean isEnabled() {
			return this.enabled;
		}

		public Duration getMinRenewal() {
			return this.minRenewal;
		}

		public Duration getExpiryThreshold() {
			return this.expiryThreshold;
		}

		public LeaseEndpoints getLeaseEndpoints() {
			return this.leaseEndpoints;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setMinRenewal(Duration minRenewal) {
			this.minRenewal = minRenewal;
		}

		public void setExpiryThreshold(Duration expiryThreshold) {
			this.expiryThreshold = expiryThreshold;
		}

		public void setLeaseEndpoints(LeaseEndpoints leaseEndpoints) {
			this.leaseEndpoints = leaseEndpoints;
		}

	}

}
