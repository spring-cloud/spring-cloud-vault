/*
 * Copyright 2018-2023 the original author or authors.
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

package org.springframework.cloud.vault.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.LifecycleAwareSessionManagerSupport;
import org.springframework.vault.authentication.ReactiveLifecycleAwareSessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.core.env.LeaseAwareVaultPropertySource;

/**
 * Runtime hints for Spring Cloud Vault usage with native images.
 *
 * @author Mark Paluch
 */
class VaultRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		ReflectionHints reflection = hints.reflection();

		reflection.registerTypes(Arrays.asList(SimpleSessionManager.class, LifecycleAwareSessionManager.class,
				LifecycleAwareSessionManagerSupport.class, ClientHttpRequestFactoryFactory.class,
				org.springframework.vault.core.env.VaultPropertySource.class, LeaseAwareVaultPropertySource.class)
				.stream().map(TypeReference::of).collect(Collectors.toList()),
				builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));

		reflection.registerTypes(
				Arrays.asList(VaultKeyValueBackendProperties.class).stream().map(TypeReference::of)
						.collect(Collectors.toList()),
				builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS,
						MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS,
						MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));

		reflection.registerType(
				TypeReference.of("org.springframework.vault.core.lease.SecretLeaseContainer$LeaseRenewalScheduler"),
				builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));

		reflection.registerType(
				TypeReference.of("org.springframework.vault.core.lease.SecretLeaseEventPublisher$LoggingErrorListener"),
				builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));

		reflection.registerType(TypeReference
				.of("org.springframework.cloud.vault.config.VaultReactiveConfiguration$ReactiveSessionManagerAdapter"),
				builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));

		if (VaultConfigDataLoader.webclientPresent && VaultConfigDataLoader.reactorPresent) {
			reflection
					.registerTypes(
							Arrays.asList(ReactiveLifecycleAwareSessionManager.class).stream().map(TypeReference::of)
									.collect(Collectors.toList()),
							builder -> builder.withMembers(MemberCategory.DECLARED_FIELDS));
		}

		reflection.registerTypeIfPresent(classLoader, "reactor.core.publisher.Flux", MemberCategory.PUBLIC_CLASSES);
		reflection.registerTypeIfPresent(classLoader, "org.springframework.web.reactive.function.client.WebClient",
				MemberCategory.PUBLIC_CLASSES);

		reflection.registerTypeIfPresent(classLoader, "org.bouncycastle.crypto.signers.PSSSigner",
				MemberCategory.PUBLIC_CLASSES);
		reflection.registerTypeIfPresent(classLoader, "com.google.api.client.googleapis.auth.oauth2.GoogleCredential",
				MemberCategory.PUBLIC_CLASSES);
		reflection.registerTypeIfPresent(classLoader, "com.google.auth.oauth2.GoogleCredentials",
				MemberCategory.PUBLIC_CLASSES);
	}

}
