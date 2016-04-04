/*
 * Copyright 2013-2015 the original author or authors.
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

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
	private String host = "127.0.0.1";

	/**
	 * Vault server port.
	 */
	@Range(min = 1, max = 65535)
	private int port = 8200;

	/**
	 * Protocol scheme. Can be either "http" or "https".
	 */
	private String scheme = "http";

	/**
	 * Name of the default backend.
	 */
	@NotEmpty
	private String backend = "secret";

	/**
	 * Name of the default context.
	 */
	@NotEmpty
	private String defaultContext = "application";

	/**
	 * Profile-separator to combine application name and profile.
	 */
	@NotEmpty
	private String profileSeparator = ",";

	/**
	 * Static vault token. Required if {@link #authentication} is {@code TOKEN}.
	 */
	private String token;

	private AppIdProperties appId = new AppIdProperties();

	private final MySql mysql = new MySql();

	private final PostgreSql postgresql = new PostgreSql();

	private final Cassandra cassandra = new Cassandra();

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
		 * @see MacAddressUserId
		 */
		public final static String MAC_ADDRESS = "MAC_ADDRESS";

		/**
		 * Property value for UserId generation using an IP-Address.
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
		 * UserId mechanism. Can be either "MAC_ADDRESS", "IP_ADDRESS". Any other values are passed as UserId.
		 */
		@NotEmpty
		private String userId = MAC_ADDRESS;
	}

	@Data
	public static class MySql implements DatabaseSecretProperties {

		/**
		 * Enable mysql backend usage.
		 */
		private boolean enabled = false;

		/**
		 * Role name for credentials.
		 */
		private String role;

		/**
		 * mysql backend path.
		 */
		@NotEmpty
		private String backend = "mysql";

		/**
		 * Target property for the obtained username.
		 */
		@NotEmpty
		private String usernameProperty = "spring.datasource.username";

		/**
		 * Target property for the obtained username.
		 */
		@NotEmpty
		private String passwordProperty = "spring.datasource.password";
	}

	@Data
	public static class PostgreSql implements DatabaseSecretProperties {

		/**
		 * Enable postgresql backend usage.
		 */
		private boolean enabled = false;

		/**
		 * Role name for credentials.
		 */
		private String role;

		/**
		 * postgresql backend path.
		 */
		@NotEmpty
		private String backend = "postgresql";

		/**
		 * Target property for the obtained username.
		 */
		@NotEmpty
		private String usernameProperty = "spring.datasource.username";

		/**
		 * Target property for the obtained username.
		 */
		@NotEmpty
		private String passwordProperty = "spring.datasource.password";
	}

	@Data
	public static class Cassandra implements DatabaseSecretProperties {

		/**
		 * Enable cassandra backend usage.
		 */
		private boolean enabled = false;

		/**
		 * Role name for credentials.
		 */
		private String role;

		/**
		 * Cassandra backend path.
		 */
		@NotEmpty
		private String backend = "cassandra";

		/**
		 * Target property for the obtained username.
		 */
		@NotEmpty
		private String usernameProperty = "spring.data.cassandra.username";

		/**
		 * Target property for the obtained password.
		 */
		@NotEmpty
		private String passwordProperty = "spring.data.cassandra.password";
	}

	/**
	 * Configuration properties for database secrets.
	 */
	public interface DatabaseSecretProperties {

		/**
		 * Role name.
		 *
		 * @return
		 */
		String getRole();

		/**
		 * Backend path.
		 *
		 * @return
		 */
		String getBackend();

		/**
		 * Name of the target property for the obtained username.
		 */
		String getUsernameProperty();

		/**
		 * Name of the target property for the obtained password.
		 */
		String getPasswordProperty();
	}

	public enum AuthenticationMethod {
		TOKEN, APPID,
	}
}
