/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.vault.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.vault.VaultContainer;

import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.VaultToken;

public class VaultRule implements BeforeEachCallback,
		/* ParameterResolver, */ ApplicationContextInitializer<ConfigurableApplicationContext> {

	static final Logger logger = LoggerFactory.getLogger(VaultRule.class);

	public static final Version VERSIONING_INTRODUCED_WITH = Version.parse("0.10.0");

	public static final VaultContainer vaultContainer = createContainer();

	private final VaultEndpoint vaultEndpoint;

	private static PrepareVault prepareVault = null;

	private VaultToken token;

	/**
	 * Create a new {@link VaultRule} with default SSL configuration and endpoint.
	 *
	 * @see Settings#createSslConfiguration()
	 * @see VaultEndpoint
	 */
	public VaultRule() {
		this(Settings.createSslConfiguration(), null);
	}

	/**
	 * Create a new {@link VaultRule} with the given {@link SslConfiguration} and
	 * {@link VaultEndpoint}.
	 * @param sslConfiguration must not be {@literal null}.
	 * @param vaultEndpoint must not be {@literal null}.
	 */
	public VaultRule(SslConfiguration sslConfiguration, VaultEndpoint vaultEndpoint) {

		Assert.notNull(sslConfiguration, "SslConfiguration must not be null");

		ClientHttpRequestFactory requestFactory = TestRestTemplateFactory.create(sslConfiguration).getRequestFactory();

		startVault();
		if (vaultEndpoint == null) {
			this.vaultEndpoint = VaultEndpoint.create(vaultContainer.getHost(), vaultContainer.getMappedPort(8200));
			// this.vaultEndpoint.setScheme("http"); // ignore ssl for now
		}
		else {
			this.vaultEndpoint = vaultEndpoint;
		}

		VaultTemplate vaultTemplate = new VaultTemplate(this.vaultEndpoint, requestFactory,
				new PreparingSessionManager());

		this.token = Settings.token();
		prepareVault = new PrepareVault(vaultTemplate);
	}

	public static PrepareVault prepare() {
		return prepareVault;
	}

	public static void startVault() {
		if (vaultContainer != null && !vaultContainer.isRunning()) {
			vaultContainer.start();
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static VaultContainer createContainer() {
		return createContainer("1.11.0");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static VaultContainer createContainer(String version) {
		Path config = Settings.findConfigDir().toPath();
		String vaultConfig = null;
		try {
			vaultConfig = new FileSystemResource(config.resolve("vaultconfig.json"))
				.getContentAsString(StandardCharsets.UTF_8);
			vaultConfig = vaultConfig.replaceAll("\\n", "");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		Path work = Settings.findWorkDir().toPath();
		String dockerImageName = "hashicorp/vault:" + version;
		VaultContainer container = (VaultContainer) new VaultContainer(dockerImageName)
			// .withEnv("VAULT_LOG_LEVEL", "debug")
			.withEnv("VAULT_LOCAL_CONFIG", vaultConfig)
			.withCopyFileToContainer(MountableFile.forHostPath(work.resolve("ca/certs/localhost.cert.pem"), 0777),
					"/tmp/localhost.cert.pem")
			.withCopyFileToContainer(
					MountableFile.forHostPath(work.resolve("ca/private/localhost.decrypted.key.pem"), 0777),
					"/tmp/localhost.decrypted.key.pem")
			.withCommand("server")
			.withLogConsumer(new Slf4jLogConsumer(logger).withSeparateOutputStreams());
		// because of the tls setup, resetting wait to default for now
		container.setWaitStrategy(Wait.defaultWaitStrategy());
		return container;
	}

	public static void initializeSystemProperties() {
		startVault();
		Integer mappedPort = vaultContainer.getMappedPort(8200);

		System.setProperty(VaultProperties.PREFIX + ".port", String.valueOf(mappedPort));
		System.setProperty(VaultProperties.PREFIX + ".host", vaultContainer.getHost());
	}

	@Override
	public void initialize(ConfigurableApplicationContext context) {
		startVault();

		MutablePropertySources sources = context.getEnvironment().getPropertySources();

		if (!sources.contains("vaultTestcontainer")) {
			Integer mappedPort = vaultContainer.getMappedPort(8200);
			HashMap<String, Object> map = new HashMap<>();
			map.put(VaultProperties.PREFIX + ".port", String.valueOf(mappedPort));
			map.put(VaultProperties.PREFIX + ".host", vaultContainer.getHost());

			sources.addFirst(new MapPropertySource("vaultTestcontainer", map));
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		before();
	}

	public void before() {

		try (Socket socket = new Socket()) {

			socket.connect(new InetSocketAddress(InetAddress.getByName("localhost"), this.vaultEndpoint.getPort()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(String.format(
					"Vault is not running on localhost:%d which is required to run a test using VaultExtension %s",
					this.vaultEndpoint.getPort(), getClass().getSimpleName()));
		}

		if (!this.prepareVault.isAvailable()) {

			this.token = this.prepareVault.initializeVault();
			this.prepareVault.awaitAvailable();
			this.prepareVault.createToken(Settings.token().getToken(), "root");

			if (this.prepareVault.getVersion().isGreaterThanOrEqualTo(VERSIONING_INTRODUCED_WITH)) {
				this.prepareVault.ensureUnversionedSecretsEngine();
				this.prepareVault.mountVersionedKvSecretsEngine();
			}

			this.token = Settings.token();
		}
	}

	/*
	 * @Override public boolean supportsParameter(ParameterContext parameterContext,
	 * ExtensionContext extensionContext) throws ParameterResolutionException { return
	 * parameterContext.getParameter().getType().equals(PrepareVault.class); }
	 *
	 * @Override public Object resolveParameter(ParameterContext parameterContext,
	 * ExtensionContext extensionContext) throws ParameterResolutionException { return
	 * this.prepareVault; }
	 */

	private class PreparingSessionManager implements SessionManager {

		@Override
		public VaultToken getSessionToken() {
			return VaultRule.this.token;
		}

	}

}
