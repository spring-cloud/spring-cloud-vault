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
package org.springframework.cloud.vault.config;

import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.IpAddressUserId;
import org.springframework.cloud.vault.TestRestTemplateFactory;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties.AppIdProperties;
import org.springframework.cloud.vault.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;

import org.junit.Before;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for {@link VaultClient} using {@link AuthenticationMethod#APPID}.
 *
 * @author Mark Paluch
 */
public class AppIdAuthenticationIntegrationTests extends GenericSecretIntegrationTests {

	@Before
	public void setUp() throws Exception {

		this.vaultProperties = Settings.createVaultProperties();

		super.setUp();

		AppIdProperties appId = configureAppIdProperties();
		vaultProperties.setApplicationName("myapp");
		vaultProperties.setAuthentication(AuthenticationMethod.APPID);
		vaultProperties.setAppId(appId);

		if (!prepare().hasAuth(appId.getAppIdPath())) {
			prepare().mountAuth(appId.getAppIdPath());
		}

		IpAddressUserId userIdMechanism = new IpAddressUserId();
		String userId = userIdMechanism.createUserId();
		prepare().mapAppId(vaultProperties.getApplicationName());
		prepare().mapUserId(vaultProperties.getApplicationName(), userId);

		RestTemplate restTemplate = TestRestTemplateFactory
				.create(vaultProperties);

		ClientAuthentication clientAuthentication = ClientAuthentication.appId(
				vaultProperties, restTemplate, userIdMechanism);

		configOperations = new VaultTemplate(vaultProperties, prepare().newVaultClient(),
				clientAuthentication).opsForConfig();
	}

	private AppIdProperties configureAppIdProperties() {

		AppIdProperties appId = new AppIdProperties();
		appId.setUserId(AppIdProperties.IP_ADDRESS);
		return appId;
	}

}
