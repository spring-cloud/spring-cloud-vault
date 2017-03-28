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
package org.springframework.cloud.vault.config;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.env.LeaseAwareVaultPropertySource;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode;
import org.springframework.vault.core.lease.event.LeaseErrorListener;
import org.springframework.vault.core.lease.event.SecretLeaseEvent;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Extension to {@link LeasingVaultPropertySourceLocator} that creates
 * {@link LeaseAwareVaultPropertySource}s.
 *
 * @author Mark Paluch
 * @see LeaseAwareVaultPropertySource
 */
@CommonsLog
class LeasingVaultPropertySourceLocator extends VaultPropertySourceLocatorSupport
		implements PriorityOrdered {

	private static final UriTemplateHandler TEMPLATE_HANDLER = new DefaultUriTemplateHandler();

	private final SecretLeaseContainer secretLeaseContainer;

	private final VaultProperties properties;

	/**
	 * Creates a new {@link LeasingVaultPropertySourceLocator}.
	 * @param properties must not be {@literal null}.
	 * @param genericBackendProperties must not be {@literal null}.
	 * @param backendAccessors must not be {@literal null}.
	 * @param secretLeaseContainer must not be {@literal null}.
	 */
	public LeasingVaultPropertySourceLocator(VaultProperties properties,
			VaultGenericBackendProperties genericBackendProperties,
			Collection<SecretBackendMetadata> backendAccessors,
			SecretLeaseContainer secretLeaseContainer) {

		super("vault", genericBackendProperties, backendAccessors);

		Assert.notNull(secretLeaseContainer, "SecretLeaseContainer must not be null");
		Assert.notNull(properties, "VaultProperties must not be null");

		this.secretLeaseContainer = secretLeaseContainer;
		this.properties = properties;
	}

	@Override
	public int getOrder() {
		return properties.getConfig().getOrder();
	}

	/**
	 * Create {@link VaultPropertySource} initialized with a
	 * {@link SecretBackendMetadata}.
	 *
	 * @param accessor the {@link SecretBackendMetadata}.
	 * @return the {@link VaultPropertySource} to use.
	 */
	protected PropertySource<?> createVaultPropertySource(
			SecretBackendMetadata accessor) {

		URI expand = TEMPLATE_HANDLER.expand("{backend}/{key}", accessor.getVariables());

		String leaseMode = accessor.getVariables().get("leaseMode");
		final RequestedSecret secret = RequestedSecret.buildFromMode(leaseMode == null ? Mode.RENEW : Mode.valueOf(leaseMode), expand.getPath());

		if (properties.isFailFast()) {
			return createVaultPropertySourceFailFast(secret, accessor);
		}

		return createVaultPropertySource(secret, accessor);
	}

	/**
	 * Decorated {@link PropertySource} creation to catch and throw the first error that
	 * occurred durin initial secret retrieval.
	 *
	 * @param secret
	 * @param accessor
	 * @return
	 */
	private PropertySource<?> createVaultPropertySourceFailFast(
			final RequestedSecret secret, SecretBackendMetadata accessor) {

		final AtomicReference<Exception> errorRef = new AtomicReference<>();

		LeaseErrorListener errorListener = new LeaseErrorListener() {
			@Override
			public void onLeaseError(SecretLeaseEvent leaseEvent, Exception exception) {

				if (leaseEvent.getSource() == secret) {
					errorRef.compareAndSet(null, exception);
				}
			}
		};

		this.secretLeaseContainer.addErrorListener(errorListener);
		try {
			return createVaultPropertySource(secret, accessor);
		}
		finally {
			this.secretLeaseContainer.removeLeaseErrorListener(errorListener);

			Exception exception = errorRef.get();
			if (exception != null) {
				if (exception instanceof VaultException) {
					throw (VaultException) exception;
				}
				throw new VaultException(
						String.format("Cannot initialize PropertySource for secret at %s",
								secret.getPath()),
						exception);
			}
		}
	}

	private PropertySource<?> createVaultPropertySource(RequestedSecret secret,
			SecretBackendMetadata accessor) {

		return new LeaseAwareVaultPropertySource(accessor.getName(),
				this.secretLeaseContainer, secret, accessor.getPropertyTransformer());
	}
}
