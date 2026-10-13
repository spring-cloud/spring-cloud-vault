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

/**
 * Support classes for Vault LDAP secret engine integration. Allows Spring Cloud Vault to
 * fetch LDAP credentials from HashiCorp Vault using either dynamic or static roles.
 * <p>
 * Dynamic roles generate temporary credentials on-demand, while static roles manage the
 * rotation of existing LDAP user credentials.
 * <p>
 * Configuration is done via {@code spring.cloud.vault.ldap} properties.
 *
 * @see org.springframework.cloud.vault.config.ldap.VaultLdapProperties
 * @see org.springframework.cloud.vault.config.ldap.VaultConfigLdapBootstrapConfiguration
 */
package org.springframework.cloud.vault.config.ldap;
