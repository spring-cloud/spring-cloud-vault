package org.springframework.cloud.vault.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link VaultPropertySourceContextStrategy} that generates the
 * following vault paths to search for configuration values:
 * <ol>
 *   <li>/{backend}/{application}/{profile}</li>
 *   <li>/{backend}/{application}</li>
 *   <li>/{backend}/{defaultContext}/{profile}</li>
 *   <li>/{backend}/{defaultContext}</li>
 * </ol>
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Jean-Philippe Bélanger
 * @author Jonathan Pearlin
 */
public class DefaultVaultPropertySourceContextStrategy implements VaultPropertySourceContextStrategy {

    private final VaultGenericBackendProperties genericBackendProperties;

    /**
     * Creates a new {@link DefaultVaultPropertySourceContextStrategy}.
     *
     * @param genericBackendProperties must not be {@literal null}.
     */
    public DefaultVaultPropertySourceContextStrategy(final VaultGenericBackendProperties genericBackendProperties) {
        Assert.notNull(genericBackendProperties,
                "VaultGenericBackendProperties must not be null");

        this.genericBackendProperties = genericBackendProperties;
    }

    @Override
    public List<String> buildContexts(final ConfigurableEnvironment env) {
        final String appName = env.getProperty("spring.application.name");
        final List<String> profiles = Arrays.asList(env.getActiveProfiles());
        final List<String> contexts = new ArrayList<>();

        final String defaultContext = genericBackendProperties.getDefaultContext();
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

    private void addProfiles(final List<String> contexts, final String baseContext,
            final List<String> profiles) {

        for (final String profile : profiles) {
            final String context = baseContext
                    + this.genericBackendProperties.getProfileSeparator() + profile;

            if (!contexts.contains(context)) {
                contexts.add(context);
            }
        }
    }
}