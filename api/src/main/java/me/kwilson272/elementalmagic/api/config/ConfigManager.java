package me.kwilson272.elementalmagic.api.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handles the main configuration files for the plugin.
 */
public interface ConfigManager {

    void enable();

    void disable(boolean shutDown);

    /**
     * Gets the {@link FileConfiguration} that corresponds to the specified
     * {@link Config} type. This method should never return null.
     *
     * @param config the {@code Config} type requested
     * @return the requested {@code FileConfiguration} object
     */
    FileConfiguration get(Config config);

    /**
     * Saves the config file that corresponds to the provided {@link Config} type.
     *
     * @param config the {@code Config} to be saved
     */
    void save(Config config);

    /**
     * Configures the provided {@link Object} with the respective values it has
     * in the config files. See the {@link Configure} annotation for information
     * on how to declare an auto-configurable field.
     *
     * @param object the {@link Object} to be configured.
     */
    void configure(Object object);
}
