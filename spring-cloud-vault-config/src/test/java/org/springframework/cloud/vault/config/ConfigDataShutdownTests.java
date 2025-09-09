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

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.vault.util.IntegrationTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.vault.core.lease.SecretLeaseContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying shutdown behavior.
 *
 * @author Mark Paluch
 */
@SpringBootApplication
public class ConfigDataShutdownTests extends IntegrationTestSupport {

	@Test
	public void contextShutdownDestroysSecretLeaseContainer() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder().sources(ConfigDataShutdownTests.class)
			.run("--server.port=0", "--spring.cloud.bootstrap.enabled=false", "--spring.cloud.vault.failFast=true",
					"--spring.cloud.vault.config.lifecycle.enabled=true", "--spring.config.import=vault://");

		SecretLeaseContainer container = context.getBean(SecretLeaseContainer.class);

		assertThat((Integer) ReflectionTestUtils.getField(container, "status")).isEqualTo(1); // started
		SpringApplication.exit(context);
		assertThat((Integer) ReflectionTestUtils.getField(container, "status")).isEqualTo(2); // destroyed
	}

}
