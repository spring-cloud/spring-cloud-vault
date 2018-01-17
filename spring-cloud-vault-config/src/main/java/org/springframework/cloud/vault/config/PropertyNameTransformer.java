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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * {@link PropertyTransformer} to transform a {@link Map} of properties by applying key
 * name translation.
 *
 * <p>
 * Existing keys will be transformed to a target key name while retaining the original
 * value. Key name translation will leave other, not specified key names untouched.
 *
 * @author Mark Paluch
 */
public class PropertyNameTransformer implements PropertyTransformer {

	private final Map<String, String> nameMapping = new HashMap<>();

	/**
	 * Create a new {@link PropertyNameTransformer}.
	 */
	public PropertyNameTransformer() {
	}

	/**
	 * Adds a key name transformation by providing a {@code sourceKeyName} and a
	 * {@code targetKeyName}.
	 *
	 * @param sourceKeyName must not be empty or {@literal null}.
	 * @param targetKeyName must not be empty or {@literal null}.
	 */
	public void addKeyTransformation(String sourceKeyName, String targetKeyName) {

		Assert.hasText(sourceKeyName, "Source key name must not be empty");
		Assert.hasText(targetKeyName, "Target key name must not be empty");

		nameMapping.put(sourceKeyName, targetKeyName);
	}

	@Override
	public Map<String, Object> transformProperties(Map<String, ? extends Object> input) {

		if (input == null) {
			return null;
		}

		Map<String, Object> transformed = new LinkedHashMap<>(input.size(), 1);

		for (String key : input.keySet()) {

			String translatedKey = key;

			if (nameMapping.containsKey(key)) {
				translatedKey = nameMapping.get(key);
			}

			transformed.put(translatedKey, input.get(key));
		}

		return transformed;
	}
}
