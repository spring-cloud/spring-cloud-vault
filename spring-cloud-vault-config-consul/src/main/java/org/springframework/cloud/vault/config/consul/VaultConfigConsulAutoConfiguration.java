/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.vault.config.consul;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bootstrap configuration providing support for the Consul secret backend.
 *
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
public class VaultConfigConsulAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConsulSecretRebindListener consulSecretRebindListener(
			ConfigurationPropertiesRebinder rebinder,
			ConfigurableApplicationContext context) {
		// TODO: some other way? Maybe a BootstrapApplicationContextHolder bean
		// provided by spring cloud commons
		ApplicationContext parent = context.getParent();
		if (parent != null) {
			context = (ConfigurableApplicationContext) parent;
		}
		return new ConsulSecretRebindListener(rebinder, context);
	}

	/**
	 * {@link SecretBackendMetadataFactory} for Consul integration using
	 * {@link VaultConsulProperties}.
	 */
	public static class ConsulSecretRebindListener
			implements ApplicationListener<ConsulBackendMetadata.RebindConsulEvent> {

		private final Log log = LogFactory.getLog(getClass());

		private final ConfigurationPropertiesRebinder rebinder;

		public ConsulSecretRebindListener(ConfigurationPropertiesRebinder rebinder,
				ConfigurableApplicationContext context) {
			this.rebinder = rebinder;
			context.addApplicationListener(this);
		}

		@Override
		public void onApplicationEvent(ConsulBackendMetadata.RebindConsulEvent event) {
			if (log.isDebugEnabled()) {
				log.debug("received RebindConsulEvent");
			}
			rebind("consulDiscoveryProperties");
			rebind("consulConfigProperties");
		}

		private void rebind(String bean) {

			boolean success = this.rebinder.rebind(bean);
			if (this.log.isInfoEnabled()) {
				this.log.info(String.format(
						"Attempted to rebind Consul bean '%s' with updated ACL token from vault, success: %s",
						bean, success));
			}
		}

	}

}
