/*
 * Copyright 2016-2022 the original author or authors.
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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.metrics.web.reactive.client.ObservationWebClientCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Reactive Observability.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Observation.class, WebClient.class, Flux.class })
@AutoConfigureBefore(VaultReactiveAutoConfiguration.class)
public class VaultReactiveObservationAutoConfiguration {

	@Bean
	@ConditionalOnSingleCandidate(ObservationRegistry.class)
	public WebClientCustomizer observationVaultWebClientCustomizer(ObservationRegistry observationRegistry) {
		return webClientBuilder -> new ObservationWebClientCustomizer(observationRegistry,
				new DefaultClientRequestObservationConvention())
			.customize(webClientBuilder);
	}

}
