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

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.cloud.vault.ClientHttpRequestFactoryFactory.*;

import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.vault.util.Settings;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.OkHttpClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for {@link ClientHttpRequestFactory}.
 *
 * @author Mark Paluch
 */
public class ClientHttpRequestFactoryFactoryIntegrationTests {

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private String url = String.format("%s://%s:%d/v1/sys/health", vaultProperties.getScheme(), vaultProperties.getHost(),
			vaultProperties.getPort());

	@Test
	public void httpComponentsClientShouldWork() throws Exception {

		ClientHttpRequestFactory factory = HttpComponents.usingHttpComponents(vaultProperties);
		RestTemplate template = new RestTemplate(factory);

		String response = request(template);

		assertThat(factory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
		assertThat(response).isNotNull().contains("initialized");

		((DisposableBean) factory).destroy();
	}

	private String request(RestTemplate template) {

		// Uninitialized and sealed can cause status 500
		try {
			ResponseEntity<String> responseEntity = template.exchange(url, HttpMethod.GET, null, String.class);
			return responseEntity.getBody();
		} catch (HttpStatusCodeException e) {
			return e.getResponseBodyAsString();
		}
	}

	@Test
	public void nettyClientShouldWork() throws Exception {

		ClientHttpRequestFactory factory = Netty.usingNetty(vaultProperties);
		((InitializingBean) factory).afterPropertiesSet();
		RestTemplate template = new RestTemplate(factory);

		String response = request(template);

		assertThat(factory).isInstanceOf(Netty4ClientHttpRequestFactory.class);
		assertThat(response).isNotNull().contains("initialized");

		((DisposableBean) factory).destroy();
	}

	@Test
	public void okHttpClientShouldWork() throws Exception {

		ClientHttpRequestFactory factory = OkHttp.usingOkHttp(vaultProperties);
		RestTemplate template = new RestTemplate(factory);

		String response = request(template);

		assertThat(factory).isInstanceOf(OkHttpClientHttpRequestFactory.class);
		assertThat(response).isNotNull().contains("initialized");

		((DisposableBean) factory).destroy();
	}
}
