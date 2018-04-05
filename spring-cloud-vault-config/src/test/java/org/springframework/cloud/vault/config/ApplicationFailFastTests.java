/*
 * Copyright 2016-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for fail fast option.
 *
 * @author Mark Paluch
 */
@SpringBootApplication
public class ApplicationFailFastTests {

	@Test
	public void contextLoadsWithFailFastUsingLeasing() {
		try {
			new SpringApplicationBuilder().sources(ApplicationFailFastTests.class).run(
					"--server.port=0", "--spring.cloud.vault.failFast=true",
					"--spring.cloud.vault.config.lifecycle.enabled=true",
					"--spring.cloud.vault.port=9999");
			fail("failFast option did not produce an exception");
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isNotEmpty();
		}
	}

	@Test
	public void contextLoadsWithFailFastWithoutLeasing() {
		try {
			new SpringApplicationBuilder().sources(ApplicationFailFastTests.class).run(
					"--server.port=0", "--spring.cloud.vault.failFast=true",
					"--spring.cloud.vault.config.lifecycle.enabled=false",
					"--spring.cloud.vault.port=9999");
			fail("failFast option did not produce an exception");
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isNotEmpty();
		}
	}

	@Test
	public void contextLoadsWithoutFailFast() {
		new SpringApplicationBuilder().sources(ApplicationFailFastTests.class).run(
				"--server.port=0", "--spring.cloud.vault.failFast=false",
				"--spring.cloud.vault.port=9999");
	}
}
