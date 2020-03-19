/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.vault.config.LeasingSecretBackendMetadata;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.util.PropertyTransformer;

/**
 * @author Mark Paluch
 */
class ConsulBackendMetadata implements LeasingSecretBackendMetadata {

	private final Log log = LogFactory.getLog(getClass());

	private final VaultConsulProperties properties;

	private final PropertyTransformer transformer;

	private final ApplicationEventPublisher eventPublisher;

	ConsulBackendMetadata(VaultConsulProperties properties,
			PropertyTransformer transformer, ApplicationEventPublisher eventPublisher) {
		this.properties = properties;
		this.transformer = transformer;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public String getName() {
		return String.format("%s with Role %s", this.properties.getBackend(),
				this.properties.getRole());
	}

	@Override
	public String getPath() {
		return String.format("%s/creds/%s", this.properties.getBackend(),
				this.properties.getRole());
	}

	@Override
	public Map<String, String> getVariables() {

		Map<String, String> variables = new HashMap<>();

		variables.put("backend", this.properties.getBackend());
		variables.put("key", String.format("creds/%s", this.properties.getRole()));

		return variables;
	}

	@Override
	public PropertyTransformer getPropertyTransformer() {
		return this.transformer;
	}

	@Override
	public RequestedSecret.Mode getLeaseMode() {
		return RequestedSecret.Mode.ROTATE;
	}

	@Override
	public void afterRegistration(RequestedSecret secret,
			SecretLeaseContainer container) {
		container.addLeaseListener(leaseEvent -> {

			if (leaseEvent.getSource() == secret
					&& leaseEvent instanceof SecretLeaseCreatedEvent) {
				if (this.eventPublisher != null) {
					if (log.isDebugEnabled()) {
						log.debug("Publishing a RebindConsulEvent");
					}
					this.eventPublisher.publishEvent(new RebindConsulEvent(this));
				}
			}
		});

		// no need to rebind here since the transformer creats all appropriate properties.
	}

	public static class RebindConsulEvent extends ApplicationEvent {

		RebindConsulEvent(Object source) {
			super(source);
		}

	}

}
