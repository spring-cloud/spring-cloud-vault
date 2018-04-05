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
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PropertyNameTransformer}.
 *
 * @author Mark Paluch
 */
public class PropertyNameTransformerUnitTests {

	@Test
	public void shouldTranslateProperties() {

		PropertyNameTransformer transformer = new PropertyNameTransformer();
		transformer.addKeyTransformation("old-key", "new-key");

		Map<String, Object> map = new HashMap<>();
		map.put("old-key", "value");
		map.put("other-key", "other-value");

		assertThat(transformer.transformProperties(map)).containsEntry("new-key", "value")
				.containsEntry("other-key", "other-value");
	}

	@Test
	public void shouldAllowNullInput() {

		PropertyNameTransformer transformer = new PropertyNameTransformer();
		transformer.addKeyTransformation("old-key", "new-key");

		assertThat(transformer.transformProperties(null)).isNull();
	}
}
