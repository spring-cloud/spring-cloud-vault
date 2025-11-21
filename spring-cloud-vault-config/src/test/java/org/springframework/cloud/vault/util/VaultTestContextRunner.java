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

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.vault.config.VaultProperties.AuthenticationMethod;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A utility class built on top of {@link SpringApplicationBuilder} that follows Spring
 * Boot’s testing patterns but designed for Spring Cloud Vault integration tests.
 *
 * <p>
 * Its goal is to eliminate repetitive boilerplate particularly when repeatedly
 * configuring:
 * </p>
 *
 * <ul>
 * <li>Bootstrap mode</li>
 * <li>Reactive vs. non-reactive modes</li>
 * <li>Environment and test classes</li>
 * <li>Vault-specific properties</li>
 * </ul>
 *
 * @author Issam EL-ATIF
 * @since 5.0.1
 * @see SpringApplicationBuilder
 * @see ApplicationContextRunner
 */
public class VaultTestContextRunner {

	private final List<Class<?>> configurationClasses = new ArrayList<>();

	private final Map<String, Object> properties = new LinkedHashMap<>();

	private VaultTestContextRunner(Map<String, Object> properties) {
		this.properties.putAll(properties);
	}

	public static VaultTestContextRunner of(AuthenticationMethod authenticationMethod) {
		Map<String, Object> authProperties = new LinkedHashMap<>();
		switch (authenticationMethod) {
			case CERT -> {
				authProperties.put("spring.cloud.vault.authentication", "cert");
				authProperties.put("spring.cloud.vault.ssl.key-store", "file:../work/client-cert.jks");
				authProperties.put("spring.cloud.vault.ssl.key-store-password", "changeit");
			}
			case KUBERNETES -> {
				authProperties.put("spring.cloud.vault.authentication", "kubernetes");
				authProperties.put("spring.cloud.vault.kubernetes.service-account-token-file",
						"../work/minikube/hello-minikube-token");
			}
		}

		return new VaultTestContextRunner(authProperties);
	}

	public VaultTestContextRunner configurations(Class<?>... configClasses) {
		this.configurationClasses.addAll(Arrays.asList(configClasses));
		return this;
	}

	public VaultTestContextRunner property(String key, Object value) {
		this.properties.put(key, value);
		return this;
	}

	public VaultTestContextRunner testClass(Class<?> testClass) {
		this.properties.put("spring.cloud.vault.application-name", testClass.getSimpleName());
		return this;
	}

	public VaultTestContextRunner bootstrap(boolean bootstrap) {
		this.properties.put("spring.cloud.bootstrap.enabled", bootstrap);
		return this;
	}

	public VaultTestContextRunner reactive(boolean reactive) {
		this.properties.put("spring.cloud.vault.reactive.enabled", reactive);
		return this;
	}

	public void run(Consumer<ConfigurableApplicationContext> consumer) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder();
		builder.sources(configurationClasses.toArray(new Class<?>[0]))
			.web(WebApplicationType.NONE)
			.properties(this.properties);

		try (ConfigurableApplicationContext context = builder.run()) {
			consumer.accept(context);
		}
	}

}
