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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.vault.ClientHttpRequestFactoryFactory.Netty;
import org.springframework.cloud.vault.ClientHttpRequestFactoryFactory.OkHttp;
import org.springframework.cloud.vault.VaultProperties.AuthenticationMethod;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.cloud.vault.util.Settings.*;

import org.assertj.core.util.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Integration tests for {@link VaultClient} using TLS certificate authentication using
 * various HTTP clients.
 *
 * @author Mark Paluch
 */
public class CertAuthenticationMethodsIntegrationTests extends AbstractIntegrationTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	VaultProperties vaultProperties = prepareCertAuthenticationMethod();

	@Before
	public void setUp() throws Exception {

		if (!prepare().hasAuth("cert")) {
			prepare().mountAuth("cert");
		}

		File workDir = findWorkDir();

		String certificate = Files.contentOf(
				new File(workDir, "ca/certs/client.cert.pem"), StandardCharsets.US_ASCII);

		prepare().write("auth/cert/certs/my-role",
				Collections.singletonMap("certificate", certificate));
	}

	@Test
	public void shouldAuthenticateUsingCertificateAuthenticationUsingHttpComponents()
			throws Exception {

		VaultClient client = new VaultClient(
				TestRestTemplateFactory
						.create(ClientHttpRequestFactoryFactory.HttpComponents
								.usingHttpComponents(vaultProperties)));

		ClientAuthentication clientAuthentication = new DefaultClientAuthentication(
				vaultProperties, client);

		assertThat(clientAuthentication.login()).isNotNull();
	}

	@Test
	public void shouldAuthenticateUsingCertificateAuthenticationUsingOkHttp()
			throws Exception {

		ClientHttpRequestFactory factory = OkHttp.usingOkHttp(vaultProperties);

		VaultClient client = new VaultClient(TestRestTemplateFactory.create(factory));

		ClientAuthentication clientAuthentication = new DefaultClientAuthentication(
				vaultProperties, client);

		assertThat(clientAuthentication.login()).isNotNull();

		((DisposableBean) factory).destroy();
	}

	@Test
	public void shouldAuthenticateUsingCertificateAuthenticationUsingNetty()
			throws Exception {

		ClientHttpRequestFactory factory = Netty.usingNetty(vaultProperties);

		VaultClient client = new VaultClient(TestRestTemplateFactory.create(factory));

		ClientAuthentication clientAuthentication = new DefaultClientAuthentication(
				vaultProperties, client);

		assertThat(clientAuthentication.login()).isNotNull();

		((DisposableBean) factory).destroy();
	}

	private VaultProperties prepareCertAuthenticationMethod() {

		VaultProperties vaultProperties = Settings.createVaultProperties();

		vaultProperties.setAuthentication(AuthenticationMethod.CERT);

		vaultProperties.getSsl().setKeyStorePassword("changeit");
		vaultProperties.getSsl().setKeyStore(
				new FileSystemResource(new File(findWorkDir(), "client-cert.jks")));

		return vaultProperties;
	}
}
