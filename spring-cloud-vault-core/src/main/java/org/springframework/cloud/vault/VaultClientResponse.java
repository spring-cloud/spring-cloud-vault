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
package org.springframework.cloud.vault;

import java.net.URI;

import org.springframework.http.HttpStatus;

import lombok.Value;

/**
 * Encapsulates the client response used in {@link VaultClient}. Consists of the body,
 * status code the location and a message. The {@code body} is empty for all
 * non-successful results.
 *
 * This class is immutable.
 *
 * @author Mark Paluch
 */
@Value(staticConstructor = "of")
public class VaultClientResponse {

	private VaultResponse body;
	private HttpStatus statusCode;
	private URI uri;
	private String message;

	/**
	 *
	 * @return {@literal true} if the request was completed successfully.
	 */
	public boolean isSuccessful() {
		return body != null && statusCode.is2xxSuccessful();
	}
}
