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

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Mechanism to generate a UserId based on the Mac address. {@link MacAddressUserId} creates a hex-encoded
 * representation of the Mac address without any separators (0123456789AB). A
 * {@link VaultProperties.AppIdProperties#networkInterface} can be
 * specified optionally to select a network interface (index/name).
 *
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor
@CommonsLog
public class MacAddressUserId implements AppIdUserIdMechanism {

	private final VaultProperties vaultProperties;

	@Override
	public String createUserId() {
		try {

			NetworkInterface networkInterface = null;
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

			VaultProperties.AppIdProperties appId = vaultProperties.getAppId();
			if (StringUtils.hasText(appId.getNetworkInterface())) {
				try {
					networkInterface = getNetworkInterface(Integer.parseInt(appId.getNetworkInterface()), interfaces);
				} catch (NumberFormatException e) {
					networkInterface = getNetworkInterface((appId.getNetworkInterface()), interfaces);
				}
			}

			if (networkInterface == null) {
				if (StringUtils.hasText(appId.getNetworkInterface())) {
					log.warn(
							String.format("Did not find a NetworkInterface applying hint %s", appId.getNetworkInterface()));
				}

				InetAddress localHost = InetAddress.getLocalHost();
				networkInterface = NetworkInterface.getByInetAddress(localHost);

				if (networkInterface == null) {
					throw new IllegalStateException(String.format("Cannot determine NetworkInterface for %s", localHost));
				}
			}

			byte[] mac = networkInterface.getHardwareAddress();
			if (mac == null) {
				throw new IllegalStateException(String.format("Network interface %s has no hardware address", networkInterface.getName()));
			}

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X", mac[i]));
			}
			return Sha256.toSha256(sb.toString());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private NetworkInterface getNetworkInterface(Number hint, List<NetworkInterface> interfaces) {

		if (interfaces.size() > hint.intValue() && hint.intValue() >= 0) {
			return interfaces.get(hint.intValue());
		}

		return null;
	}

	private NetworkInterface getNetworkInterface(String hint, List<NetworkInterface> interfaces) {

		for (NetworkInterface anInterface : interfaces) {
			if (hint.equals(anInterface.getDisplayName()) || hint.equals(anInterface.getName())) {
				return anInterface;
			}
		}

		return null;
	}
}
