package org.springframework.cloud.vault.config;

import java.util.List;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Defines a strategy for building the list of Vault contexts to be searched
 * by the {@link VaultPropertySourceLocator} for configuration.
 *
 * @author Jonathan Pearlin
 */
public interface VaultPropertySourceContextStrategy {

    /**
     * Builds the Vault context paths used by the {@link VaultPropertySourceLocator}
     * to search for configuration.
     * @param env The selected Spring environment.
     * @return The list of Vault context paths.
     */
    List<String> buildContexts(ConfigurableEnvironment env);
}
