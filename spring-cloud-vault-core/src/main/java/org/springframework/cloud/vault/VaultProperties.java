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

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import lombok.Data;

/**
 * @author Spencer Gibb
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault")
@Data
public class VaultProperties {

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
	 * Connection timeout;
	 */
	private int connectionTimeout = 5000;

	/**
	 * Read timeout;
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

	private AwsEc2Properties awsEc2 = new AwsEc2Properties();

	private Ssl ssl = new Ssl();

	/**
	 * Application name for AppId authentication.
	 */
	@org.springframework.beans.factory.annotation.Value("${spring.application.name:application}")
	private String applicationName;

	private AuthenticationMethod authentication = AuthenticationMethod.TOKEN;

	@Data
	public static class AppIdProperties {

		/**
		 * Property value for UserId generation using a Mac-Address.
		 * 
		 * @see MacAddressUserId
		 */
		public final static String MAC_ADDRESS = "MAC_ADDRESS";

		/**
		 * Property value for UserId generation using an IP-Address.
		 * 
		 * @see IpAddressUserId
		 */
		public final static String IP_ADDRESS = "IP_ADDRESS";

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

	@Data
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
		 * Flag whether to generate and send a nonce.
		 */
		private boolean useNonce = true;
	}

	@Data
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

	public enum AuthenticationMethod {
		TOKEN, APPID, AWS_EC2, CERT
	}
}
