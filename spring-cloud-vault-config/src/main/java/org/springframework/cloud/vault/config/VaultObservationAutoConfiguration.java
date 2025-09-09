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

package org.springframework.cloud.vault.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.vault.client.RestTemplateCustomizer;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Observability.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Observation.class)
@AutoConfigureBefore(VaultAutoConfiguration.class)
public class VaultObservationAutoConfiguration {

	@Bean
	@ConditionalOnSingleCandidate(ObservationRegistry.class)
	public RestTemplateCustomizer observationVaultRestTemplateCustomizer(ObservationRegistry observationRegistry) {
		return restTemplate -> new ObservationRestTemplateCustomizer(observationRegistry,
				new DefaultClientRequestObservationConvention())
			.customize(restTemplate);
	}

}
