/*
 * Copyright 2018-2019 the original author or authors.
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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.lang.Nullable;
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

	private static Health getHealth(Builder builder,
			VaultHealthImpl vaultHealthResponse) {

		HealthBuilderDelegate.contributeToHealth(vaultHealthResponse, builder);
		return builder.build();
	}

	@Override
	protected Mono<Health> doHealthCheck(Builder builder) {

		return this.vaultOperations
				.doWithSession((it) -> it.get().uri("sys/health").exchange())
				.flatMap((it) -> it.bodyToMono(VaultHealthImpl.class))
				.onErrorResume(WebClientResponseException.class,
						VaultReactiveHealthIndicator::deserializeError)
				.map((vaultHealthResponse) -> getHealth(builder, vaultHealthResponse));
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static class VaultHealthImpl implements VaultHealth {

		private final boolean initialized;

		private final boolean sealed;

		private final boolean standby;

		private final boolean performanceStandby;

		private final boolean replicationRecoverySecondary;

		private final int serverTimeUtc;

		@Nullable
		private final String version;

		VaultHealthImpl(@JsonProperty("initialized") boolean initialized,
				@JsonProperty("sealed") boolean sealed,
				@JsonProperty("standby") boolean standby,
				@JsonProperty("performance_standby") boolean performanceStandby,
				@Nullable @JsonProperty("replication_dr_mode") String replicationRecoverySecondary,
				@JsonProperty("server_time_utc") int serverTimeUtc,
				@Nullable @JsonProperty("version") String version) {

			this.initialized = initialized;
			this.sealed = sealed;
			this.standby = standby;
			this.performanceStandby = performanceStandby;
			this.replicationRecoverySecondary = replicationRecoverySecondary != null
					&& !"disabled".equalsIgnoreCase(replicationRecoverySecondary);
			this.serverTimeUtc = serverTimeUtc;
			this.version = version;
		}

		public boolean isInitialized() {
			return this.initialized;
		}

		public boolean isSealed() {
			return this.sealed;
		}

		public boolean isStandby() {
			return this.standby;
		}

		public boolean isPerformanceStandby() {
			return this.performanceStandby;
		}

		public boolean isRecoveryReplicationSecondary() {
			return this.replicationRecoverySecondary;
		}

		public int getServerTimeUtc() {
			return this.serverTimeUtc;
		}

		@Nullable
		public String getVersion() {
			return this.version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof VaultHealthImpl)) {
				return false;
			}
			VaultHealthImpl that = (VaultHealthImpl) o;
			return this.initialized == that.initialized && this.sealed == that.sealed
					&& this.standby == that.standby
					&& this.performanceStandby == that.performanceStandby
					&& this.replicationRecoverySecondary == that.replicationRecoverySecondary
					&& this.serverTimeUtc == that.serverTimeUtc
					&& Objects.equals(this.version, that.version);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.initialized, this.sealed, this.standby,
					this.performanceStandby, this.replicationRecoverySecondary,
					this.serverTimeUtc, this.version);
		}

	}

}
