/*
 * Copyright 2025-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.vault.config.VaultProperties.AuthenticationMethod;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A utility class built on top of {@link SpringApplicationBuilder} that follows Spring
 * Boot’s testing patterns but designed for Spring Cloud Vault integration tests.
 *
 * <p>
 * Its goal is to eliminate repetitive boilerplate particularly when repeatedly
 * configuring startup properties.
 *
 * @author Issam EL-ATIF
 * @author Mark Paluch
 * @see SpringApplicationBuilder
 * @see ApplicationContextRunner
 */
public final class VaultTestContextRunner {

	private final List<Class<?>> configurationClasses = new ArrayList<>();

	private final Map<String, Object> properties = new LinkedHashMap<>();

	private final TestSettings testSettings = new TestSettings();

	private VaultTestContextRunner(Map<String, Object> properties) {
		this.properties.putAll(properties);
	}

	/**
	 * Create a {@code VaultTestContextRunner} with default TLS configuration.
	 * @return the {@link VaultTestContextRunner}.
	 */
	public static VaultTestContextRunner create() {

		Map<String, Object> configurationProperties = new LinkedHashMap<>();
		configurationProperties.put("spring.cloud.vault.ssl.key-store", "file:../work/client-cert.jks");
		configurationProperties.put("spring.cloud.vault.ssl.key-store-password", "changeit");

		return new VaultTestContextRunner(configurationProperties);
	}

	/**
	 * Create a {@code VaultTestContextRunner} with default TLS configuration and using
	 * the {@code testClass} to set the application name.
	 * @return the {@link VaultTestContextRunner}.
	 */
	public static VaultTestContextRunner of(Class<?> testClass) {
		return create().testClass(testClass);
	}

	/**
	 * Configure an authentication method to be used.
	 * @param authenticationMethod the authentication method to use.
	 * @return this builder.
	 */
	public VaultTestContextRunner auth(AuthenticationMethod authenticationMethod) {
		return property("spring.cloud.vault.authentication", authenticationMethod.name());
	}

	/**
	 * Configure the application name based on the given test class.
	 * @param testClass the test class.
	 * @return this builder.
	 */
	public VaultTestContextRunner testClass(Class<?> testClass) {
		return property("spring.cloud.vault.application-name", testClass.getSimpleName());
	}

	/**
	 * Configure configuration classes to be added.
	 * @param configClasses configuration classes to add.
	 * @return this builder.
	 */
	public VaultTestContextRunner configurations(Class<?>... configClasses) {
		this.configurationClasses.addAll(Arrays.asList(configClasses));
		return this;
	}

	/**
	 * Set a property to be used in the test context.
	 * @param key the property key.
	 * @param value the property value.
	 * @return this builder.
	 */
	public VaultTestContextRunner property(String key, Object value) {
		this.properties.put(key, value);
		return this;
	}

	/**
	 * Provides access to {@link TestSettings} with the possibility to update test
	 * settings.
	 * @param settingsConsumer a function that consumes the test settings.
	 * @return this builder.
	 */
	public VaultTestContextRunner settings(Consumer<TestSettings> settingsConsumer) {
		settingsConsumer.accept(this.testSettings);
		return this;
	}

	/**
	 * Run the application and provide access to the application context through the
	 * {@code consumer}.
	 * @param consumer consumes the application context.
	 */
	public void run(Consumer<ConfigurableApplicationContext> consumer) {

		property("spring.cloud.bootstrap.enabled", this.testSettings.bootstrap);
		property("spring.cloud.vault.reactive.enabled", this.testSettings.reactive);

		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		builder.sources(this.configurationClasses.toArray(new Class<?>[0]))
			.web(WebApplicationType.NONE)
			.properties(this.properties);

		try (ConfigurableApplicationContext context = builder.run()) {
			consumer.accept(context);
		}
	}

	/**
	 * Common settings used in tests.
	 */
	public class TestSettings {

		private boolean bootstrap = false;

		private boolean reactive = false;

		/**
		 * Enable Spring Cloud bootstrap context usage (disabled by default).
		 * @return this settings builder.
		 */
		public TestSettings bootstrap() {
			return bootstrap(true);
		}

		/**
		 * Enable or disable Spring Cloud bootstrap context usage (disabled by default).
		 * @return this settings builder.
		 */
		public TestSettings bootstrap(boolean bootstrap) {
			this.bootstrap = bootstrap;
			return this;
		}

		/**
		 * Enable reactive usage (disabled by default).
		 * @return this settings builder.
		 */
		public TestSettings reactive() {
			return reactive(true);
		}

		/**
		 * Enable or disable reactive usage (disabled by default).
		 * @return this settings builder.
		 */
		public TestSettings reactive(boolean reactive) {
			this.reactive = reactive;
			return this;
		}

	}

}
