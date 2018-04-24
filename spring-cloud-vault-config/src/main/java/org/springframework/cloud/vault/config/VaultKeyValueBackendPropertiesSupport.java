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

/**
 * Interface declaring Key-Value configuration properties.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public interface VaultKeyValueBackendPropertiesSupport {

	/**
	 * @return {@literal true} if this backend configuration is enabled; {@literal false}
	 * otherwise.
	 */
	boolean isEnabled();

	/**
	 * @return mound path of the secret backend.
	 */
	String getBackend();

	/**
	 * @return default context path. Can be empty.
	 */
	String getDefaultContext();

	/**
	 * Profile separator character.
	 */
	String getProfileSeparator();

	/**
	 * @return the application name to use.
	 */
	String getApplicationName();

}
