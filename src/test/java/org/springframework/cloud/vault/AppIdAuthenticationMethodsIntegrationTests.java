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

import static org.assertj.core.api.Assertions.*;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.vault.VaultProperties.AppIdProperties;
import org.springframework.cloud.vault.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;

/**
 * Integration tests for {@link VaultClient} using various UserIds.
 *
 * @author Mark Paluch
 */
public class AppIdAuthenticationMethodsIntegrationTests extends AbstractIntegrationTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		prepare().mapAppId("myapp");
	}

	@Test
	public void loginUsingIpAddressShouldCreateAToken() throws Exception {

		VaultClient vaultClient = new VaultClient(
				prepareAppIdAuthenticationMethod(AppIdProperties.IP_ADDRESS, "myapp"));
		vaultClient.setAppIdUserIdMechanism(new IpAddressUserId());
		assertThat(vaultClient.createToken()).isNotNull();
	}

	@Test
	public void loginUsingStaticUserIdShouldCreateAToken() throws Exception {

		VaultProperties vaultProperties = prepareAppIdAuthenticationMethod("my-user-id",
				"myapp");
		VaultClient vaultClient = new VaultClient(vaultProperties);
		vaultClient.setAppIdUserIdMechanism(new StaticUserId(vaultProperties));
		assertThat(vaultClient.createToken()).isNotNull();
	}

	@Test
	public void loginUsingMacAddressShouldCreateAToken() throws Exception {

		VaultProperties vaultProperties = prepareAppIdAuthenticationMethod(
				AppIdProperties.MAC_ADDRESS, "myapp");
		VaultClient vaultClient = new VaultClient(vaultProperties);

		vaultClient.setAppIdUserIdMechanism(new MacAddressUserId(vaultProperties));
		assertThat(vaultClient.createToken()).isNotNull();
	}

	@Test
	public void invalidLogin() throws Exception {

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("Cannot login using app-id");

		VaultProperties vaultProperties = prepareAppIdAuthenticationMethod(
				AppIdProperties.IP_ADDRESS, "myapp");
		vaultProperties.setApplicationName("foobar");

		VaultClient vaultClient = new VaultClient(vaultProperties);
		vaultClient.setAppIdUserIdMechanism(new MacAddressUserId(vaultProperties));

		vaultClient.createToken();

		fail("Missing IllegalStateException");

	}

	private VaultProperties prepareAppIdAuthenticationMethod(String userId, String appId)
			throws SocketException {

		VaultProperties vaultProperties = Settings.createVaultProperties();

		AppIdProperties appIdProperties = new AppIdProperties();
		vaultProperties.setApplicationName(appId);
		appIdProperties.setUserId(userId);

		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
				.getNetworkInterfaces();
		NetworkInterface networkInterface = null;
		while (networkInterfaces.hasMoreElements()) {
			networkInterface = networkInterfaces.nextElement();
			if (networkInterface.getHardwareAddress() != null) {
				break;
			}
		}

		// make sure we have always a network interface even if the localhost reverse
		// lookup maps to an IP address that is not handled by this host.
		appIdProperties.setNetworkInterface(networkInterface.getName());

		vaultProperties.setAuthentication(AuthenticationMethod.APPID);
		vaultProperties.setAppId(appIdProperties);

		String userIdValue;
		if (userId.equals(AppIdProperties.IP_ADDRESS)) {
			userIdValue = new IpAddressUserId().createUserId();
		}
		else if (userId.equals(AppIdProperties.MAC_ADDRESS)) {
			userIdValue = new MacAddressUserId(vaultProperties).createUserId();
		}
		else {
			userIdValue = userId;
		}

		prepare().mapUserId(vaultProperties.getApplicationName(), userIdValue);

		return vaultProperties;
	}
}
