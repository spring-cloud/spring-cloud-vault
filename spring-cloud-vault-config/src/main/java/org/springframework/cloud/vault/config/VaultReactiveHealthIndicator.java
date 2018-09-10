/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.vault.config;

import java.lang.reflect.UndeclaredThrowableException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.ReactiveVaultOperations;
import org.springframework.vault.support.VaultHealth;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Reactive health indicator reporting Vault's availability.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class VaultReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveVaultOperations vaultOperations;

	public VaultReactiveHealthIndicator(ReactiveVaultOperations vaultOperations) {
		this.vaultOperations = vaultOperations;
	}

	@Override
	protected Mono<Health> doHealthCheck(Builder builder) {

		return vaultOperations
				.doWithSession(it -> it.get().uri("sys/health").exchange())
				.flatMap(it -> it.bodyToMono(VaultHealthImpl.class))
				.onErrorResume(WebClientResponseException.class,
						VaultReactiveHealthIndicator::deserializeError)
				.map(vaultHealthResponse -> {
					return getHealth(builder, vaultHealthResponse);
				});
	}

	private static Mono<? extends VaultHealthImpl> deserializeError(
			WebClientResponseException e) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			return Mono.just(mapper.readValue(e.getResponseBodyAsByteArray(),
					VaultHealthImpl.class));
		}
		catch (Exception jsonError) {
			UndeclaredThrowableException t = new UndeclaredThrowableException(jsonError);
			t.addSuppressed(e);
			return Mono.error(t);
		}
	}

	private static Health getHealth(Builder builder, VaultHealthImpl vaultHealthResponse) {

		if (!vaultHealthResponse.isInitialized()) {
			builder.withDetail("state", "Vault uninitialized");
		}
		else

		if (vaultHealthResponse.isSealed()) {
			builder.down().withDetail("state", "Vault sealed");
		}
		else

		if (vaultHealthResponse.isStandby()) {
			builder.up().withDetail("state", "Vault in standby");
		}
		else {
			builder.up();
		}

		if (StringUtils.hasText(vaultHealthResponse.getVersion())) {
			builder.withDetail("version", vaultHealthResponse.getVersion());
		}
		return builder.build();
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class VaultHealthImpl implements VaultHealth {

		private final boolean initialized;
		private final boolean sealed;
		private final boolean standby;
		private final int serverTimeUtc;

		@Nullable
		private final String version;

		private VaultHealthImpl(@JsonProperty("initialized") boolean initialized,
				@JsonProperty("sealed") boolean sealed,
				@JsonProperty("standby") boolean standby,
				@JsonProperty("server_time_utc") int serverTimeUtc,
				@Nullable @JsonProperty("version") String version) {

			this.initialized = initialized;
			this.sealed = sealed;
			this.standby = standby;
			this.serverTimeUtc = serverTimeUtc;
			this.version = version;
		}
	}
}
