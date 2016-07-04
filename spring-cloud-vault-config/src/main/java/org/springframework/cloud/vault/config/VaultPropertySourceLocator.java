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

import static org.springframework.cloud.vault.config.SecureBackendAccessors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySourceLocator} using {@link VaultClient}.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 */
class VaultPropertySourceLocator implements PropertySourceLocator {

    private final VaultClient vaultClient;
    private final ClientAuthentication clientAuthentication;
    private final VaultProperties properties;
    private final VaultGenericBackendProperties genericBackendProperties;
    private final Collection<SecureBackendAccessor> backendAccessors;

    private transient final VaultState vaultState = new VaultState();

    /**
     * Creates a new {@link VaultPropertySourceLocator}.
     * 
     * @param vaultClient must not be {@literal null}.
     * @param clientAuthentication must not be {@literal null}.
     * @param properties must not be {@literal null}.
     * @param genericBackendProperties must not be {@literal null}.
     * @param backendAccessors must not be {@literal null}.
     */
    public VaultPropertySourceLocator(VaultClient vaultClient, ClientAuthentication clientAuthentication,
            VaultProperties properties, VaultGenericBackendProperties genericBackendProperties,
            Collection<SecureBackendAccessor> backendAccessors) {

        Assert.notNull(vaultClient, "VaultClient must not be null");
        Assert.notNull(clientAuthentication, "ClientAuthentication must not be null");
        Assert.notNull(properties, "VaultProperties must not be null");
        Assert.notNull(backendAccessors, "BackendAccessors must not be null");
        Assert.notNull(genericBackendProperties, "VaultGenericBackendProperties must not be null");

        this.vaultClient = vaultClient;
        this.clientAuthentication = clientAuthentication;
        this.properties = properties;
        this.backendAccessors = backendAccessors;
        this.genericBackendProperties = genericBackendProperties;
    }

    @Override
    public PropertySource<?> locate(Environment environment) {

        if (environment instanceof ConfigurableEnvironment) {

            CompositePropertySource propertySource = createCompositePropertySource((ConfigurableEnvironment) environment);
            initialize(propertySource);

            return propertySource;
        }
        return null;
    }

    private List<String> buildContexts(ConfigurableEnvironment env) {

        String appName = env.getProperty("spring.application.name");
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        List<String> contexts = new ArrayList<>();

        String defaultContext = genericBackendProperties.getDefaultContext();
        if (StringUtils.hasText(defaultContext)) {
            contexts.add(defaultContext);
        }

        addProfiles(contexts, defaultContext, profiles);

        if (StringUtils.hasText(appName)) {

            if (!contexts.contains(appName)) {
                contexts.add(appName);
            }

            addProfiles(contexts, appName, profiles);
        }

        Collections.reverse(contexts);
        return contexts;
    }

    protected CompositePropertySource createCompositePropertySource(ConfigurableEnvironment environment) {

        CompositePropertySource propertySource = new CompositePropertySource("vault");

        if (genericBackendProperties.isEnabled()) {

            List<String> contexts = buildContexts(environment);
            for (String propertySourceContext : contexts) {

                if (StringUtils.hasText(propertySourceContext)) {

                    VaultPropertySource vaultPropertySource = createVaultPropertySource(
                            generic(genericBackendProperties.getBackend(), propertySourceContext));

                    propertySource.addPropertySource(vaultPropertySource);
                }
            }
        }

        for (SecureBackendAccessor backendAccessor : backendAccessors) {

            VaultPropertySource vaultPropertySource = createVaultPropertySource(backendAccessor);
            propertySource.addPropertySource(vaultPropertySource);
        }
        return propertySource;
    }

    protected void initialize(CompositePropertySource propertySource) {

        for (PropertySource<?> source : propertySource.getPropertySources()) {
            ((VaultPropertySource) source).init();
        }
    }

    private VaultPropertySource createVaultPropertySource(SecureBackendAccessor accessor) {
        return new VaultPropertySource(this.vaultClient, this.clientAuthentication, this.properties, this.vaultState, accessor);
    }

    private void addProfiles(List<String> contexts, String baseContext, List<String> profiles) {

        for (String profile : profiles) {
            String context = baseContext + this.genericBackendProperties.getProfileSeparator() + profile;

            if (!contexts.contains(context)) {
                contexts.add(context);
            }
        }
    }
}