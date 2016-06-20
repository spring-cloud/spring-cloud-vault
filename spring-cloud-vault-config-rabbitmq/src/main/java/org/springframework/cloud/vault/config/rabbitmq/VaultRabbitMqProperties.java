/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.vault.config.rabbitmq;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.vault.VaultSecretBackend;

import lombok.Data;

/**
 * Configuration properties for Vault using the RabbitMQ integration.
 *
 * @author Mark Paluch
 */
@ConfigurationProperties("spring.cloud.vault.rabbitmq")
@Data
public class VaultRabbitMqProperties implements VaultSecretBackend {

	/**
	 * Enable rabbitmq backend usage.
	 */
	private boolean enabled = false;

	/**
	 * Role name for credentials.
	 */
	private String role;

	/**
	 * RabbitMQ backend path.
	 */
	@NotEmpty
	private String backend = "rabbitmq";

	/**
	 * Target property for the obtained username.
	 */
	@NotEmpty
	private String usernameProperty = "spring.rabbitmq.username";

	/**
	 * Target property for the obtained password.
	 */
	@NotEmpty
	private String passwordProperty = "spring.rabbitmq.password";
}