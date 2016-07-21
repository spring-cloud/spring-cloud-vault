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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.cloud.vault.VaultProperties.Ssl;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.OkHttpClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.squareup.okhttp.OkHttpClient;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.apachecommons.CommonsLog;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

/**
 * Factory for {@link ClientHttpRequestFactory} that supports Apache HTTP Components,
 * OkHttp, Netty and the JDK HTTP client (in that order). This factory configures a
 * {@link ClientHttpRequestFactory} depending on the available dependencies.
 *
 * @author Mark Paluch
 */
@CommonsLog
class ClientHttpRequestFactoryFactory {

	private final static boolean HTTP_COMPONENTS_PRESENT = ClassUtils.isPresent(
			"org.apache.http.client.HttpClient",
			ClientHttpRequestFactoryFactory.class.getClassLoader());

	private final static boolean OKHTTP_PRESENT = ClassUtils.isPresent(
			"com.squareup.okhttp.OkHttpClient",
			ClientHttpRequestFactoryFactory.class.getClassLoader());

	private final static boolean NETTY_PRESENT = ClassUtils.isPresent(
			"io.netty.channel.nio.NioEventLoopGroup",
			ClientHttpRequestFactoryFactory.class.getClassLoader());

	/**
	 * Creates a {@link ClientHttpRequestFactory} for the given {@link VaultProperties}.
	 *
	 * @param vaultProperties must not be {@literal null}
	 * @return a new {@link ClientHttpRequestFactory}. Lifecycle beans must be initialized
	 * after obtaining.
	 */
	public static ClientHttpRequestFactory create(VaultProperties vaultProperties) {

		try {

			if (HTTP_COMPONENTS_PRESENT) {
				return HttpComponents.usingHttpComponents(vaultProperties);
			}

			if (OKHTTP_PRESENT) {
				return OkHttp.usingOkHttp(vaultProperties);
			}

			if (NETTY_PRESENT) {
				return Netty.usingNetty(vaultProperties);
			}

		}
		catch (IOException | GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}

		if (hasSslConfiguration(vaultProperties)) {
			log.warn("VaultProperties has SSL configured but the SSL configuration "
					+ "must be applied outside the Vault Client to use the JDK HTTP client");
		}

		return new SimpleClientHttpRequestFactory();
	}

	private static SSLContext getSSLContext(VaultProperties.Ssl ssl)
			throws GeneralSecurityException, IOException {

		KeyManager[] keyManagers = ssl.getKeyStore() != null ? createKeyManagerFactory(
				ssl.getKeyStore(), ssl.getKeyStorePassword()).getKeyManagers() : null;

		TrustManager[] trustManagers = ssl.getTrustStore() != null ? createTrustManagerFactory(
				ssl.getTrustStore(), ssl.getTrustStorePassword()).getTrustManagers()
				: null;

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagers, trustManagers, null);

		return sslContext;
	}

	private static KeyManagerFactory createKeyManagerFactory(Resource keystoreFile,
			String storePassword) throws GeneralSecurityException, IOException {

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

		try (InputStream inputStream = keystoreFile.getInputStream()) {
			keyStore.load(inputStream,
					StringUtils.hasText(storePassword) ? storePassword.toCharArray()
							: null);
		}

		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore,
				StringUtils.hasText(storePassword) ? storePassword.toCharArray()
						: new char[0]);

		return keyManagerFactory;
	}

	private static TrustManagerFactory createTrustManagerFactory(Resource trustFile,
			String storePassword) throws GeneralSecurityException, IOException {

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

		try (InputStream inputStream = trustFile.getInputStream()) {
			trustStore.load(inputStream,
					StringUtils.hasText(storePassword) ? storePassword.toCharArray()
							: null);
		}

		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		return trustManagerFactory;
	}

	private static boolean hasSslConfiguration(VaultProperties vaultProperties) {

		Ssl ssl = vaultProperties.getSsl();

		if (ssl == null) {
			return false;
		}

		return ssl.getTrustStore() != null || ssl.getKeyStore() != null;
	}

	/**
	 * {@link ClientHttpRequestFactory} for Apache Http Components.
	 *
	 * @author Mark Paluch
	 */
	static class HttpComponents {

		static ClientHttpRequestFactory usingHttpComponents(
				VaultProperties vaultProperties) throws GeneralSecurityException,
				IOException {

			HttpClientBuilder httpClientBuilder = HttpClients.custom();

			if (hasSslConfiguration(vaultProperties)) {

				SSLContext sslContext = getSSLContext(vaultProperties.getSsl());
				SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
						sslContext);
				httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
				httpClientBuilder.setSSLContext(sslContext);
			}

			RequestConfig requestConfig = RequestConfig.custom() //
					.setConnectTimeout(vaultProperties.getConnectionTimeout()) //
					.setSocketTimeout(vaultProperties.getReadTimeout()) //
					.setAuthenticationEnabled(true) //
					.build();

			httpClientBuilder.setDefaultRequestConfig(requestConfig);

			return new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
		}
	}

	/**
	 * {@link ClientHttpRequestFactory} for the {@link OkHttpClient}.
	 *
	 * @author Mark Paluch
	 */
	static class OkHttp {

		static ClientHttpRequestFactory usingOkHttp(VaultProperties vaultProperties)
				throws GeneralSecurityException, IOException {

			final OkHttpClient okHttpClient = new OkHttpClient();

			OkHttpClientHttpRequestFactory requestFactory = new OkHttpClientHttpRequestFactory(
					okHttpClient) {

				@Override
				public void destroy() throws Exception {

					if (okHttpClient.getCache() != null) {
						okHttpClient.getCache().close();
					}

					okHttpClient.getDispatcher().getExecutorService().shutdown();
				}
			};

			if (hasSslConfiguration(vaultProperties)) {
				okHttpClient.setSslSocketFactory(getSSLContext(vaultProperties.getSsl())
						.getSocketFactory());
			}

			requestFactory.setConnectTimeout(vaultProperties.getConnectionTimeout());
			requestFactory.setReadTimeout(vaultProperties.getReadTimeout());

			return requestFactory;
		}
	}

	/**
	 * {@link ClientHttpRequestFactory} for Netty.
	 *
	 * @author Mark Paluch
	 */
	static class Netty {

		static ClientHttpRequestFactory usingNetty(VaultProperties vaultProperties)
				throws GeneralSecurityException, IOException {

			VaultProperties.Ssl ssl = vaultProperties.getSsl();

			final Netty4ClientHttpRequestFactory requestFactory = new Netty4ClientHttpRequestFactory();

			if (hasSslConfiguration(vaultProperties)) {

				SslContextBuilder sslContextBuilder = SslContextBuilder //
						.forClient();

				if (ssl.getTrustStore() != null) {
					sslContextBuilder.trustManager(createTrustManagerFactory(
							ssl.getTrustStore(), ssl.getTrustStorePassword()));
				}

				if (ssl.getKeyStore() != null) {
					sslContextBuilder.keyManager(createKeyManagerFactory(
							ssl.getKeyStore(), ssl.getKeyStorePassword()));
				}

				requestFactory.setSslContext(sslContextBuilder.sslProvider(
						SslProvider.JDK).build());
			}

			requestFactory.setConnectTimeout(vaultProperties.getConnectionTimeout());
			requestFactory.setReadTimeout(vaultProperties.getReadTimeout());

			return requestFactory;
		}
	}
}
