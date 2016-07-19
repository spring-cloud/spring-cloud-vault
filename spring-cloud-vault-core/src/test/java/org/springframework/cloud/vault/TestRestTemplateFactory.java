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

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;

import lombok.SneakyThrows;

/**
 * Factory for {@link TestRestTemplate}. The template caches the
 * {@link ClientHttpRequestFactory} once it was initialized. Changes to timeouts or the
 * SSL configuration won't be applied once a {@link ClientHttpRequestFactory} was created
 * for the first time.
 * @author Mark Paluch
 */
public class TestRestTemplateFactory {

	private final static AtomicReference<ClientHttpRequestFactory> factoryCache = new AtomicReference<>();

	/**
	 * @param vaultProperties
	 * @return
	 */
	@SneakyThrows
	public static TestRestTemplate create(VaultProperties vaultProperties) {

		initializeClientHttpRequestFactory(vaultProperties);

		TestRestTemplate testRestTemplate = new TestRestTemplate();
		testRestTemplate.setErrorHandler(new DefaultResponseErrorHandler());
		testRestTemplate.setRequestFactory(factoryCache.get());

		return testRestTemplate;
	}

	private static void initializeClientHttpRequestFactory(
			VaultProperties vaultProperties) throws Exception {

		if (factoryCache.get() != null) {
			return;
		}

		final ClientHttpRequestFactory clientHttpRequestFactory = ClientHttpRequestFactoryFactory
				.create(vaultProperties);

		if (factoryCache.compareAndSet(null, clientHttpRequestFactory)) {

			if (clientHttpRequestFactory instanceof InitializingBean) {
				((InitializingBean) clientHttpRequestFactory).afterPropertiesSet();
			}

			if (clientHttpRequestFactory instanceof DisposableBean) {

				Runtime.getRuntime().addShutdownHook(
						new Thread("ClientHttpRequestFactory Shutdown Hook") {

							@Override
							public void run() {
								try {
									((DisposableBean) clientHttpRequestFactory).destroy();
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
			}
		}
	}
}
