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

import org.springframework.util.Assert;

/**
 * Static Token-based client authentication method.
 *
 * @author Mark Paluch
 */
class TokenClientAuthentication extends ClientAuthentication {

	private final VaultProperties vaultProperties;

	TokenClientAuthentication(VaultProperties vaultProperties) {

		Assert.notNull(vaultProperties);
		Assert.isTrue(
				vaultProperties
						.getAuthentication() == VaultProperties.AuthenticationMethod.TOKEN,
				String.format("Authentication must be Token but is %s",
						vaultProperties.getAuthentication()));
		Assert.hasText(vaultProperties.getToken(), "Token must not be empty");

		this.vaultProperties = vaultProperties;
	}

	@Override
	public VaultToken login() {
		return VaultToken.of(vaultProperties.getToken());
	}
}
