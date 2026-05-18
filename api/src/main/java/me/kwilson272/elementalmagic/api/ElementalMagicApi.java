package me.kwilson272.elementalmagic.api;

import java.util.logging.Logger;

import me.kwilson272.elementalmagic.api.ability.AbilityManager;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.activation.ActivationManager;
import me.kwilson272.elementalmagic.api.config.ConfigManager;
import me.kwilson272.elementalmagic.api.user.UserManager;

public final class ElementalMagicApi {
    
    private static AbilityManager abilityManager = null;
    private static AbilityStorage abilityStorage = null;
    private static ActivationManager activationManager = null;
    private static ConfigManager configManager = null;
    private static Logger logger = null;
    private static ElementalMagicPlugin emPlugin = null;
    private static UserManager userManager = null;

    public static AbilityManager abilityManager() {
        return abilityManager;
    }

    public static AbilityStorage abilityStorage() {
        return abilityStorage;
    }

    public static ActivationManager activationManager() {
        return activationManager;
    }

    public static ConfigManager configManager() {
        return configManager;
    }

    public static Logger logger() {
        return logger;
    }

    public static ElementalMagicPlugin plugin() {
        return emPlugin;
    }

    public static UserManager userManager() {
        return userManager;
    }

    public static void registerAbilityManager(AbilityManager manager) {
        if (abilityManager != null) {
            throw new IllegalStateException("An AbilityManager instance has " +
                    "already been registered with class: " + abilityManager.getClass());
        }
        abilityManager = manager;
    }

    public static void registerAbilityStorage(AbilityStorage storage) {
        if (abilityStorage != null) {
            throw new IllegalStateException("An AbilityStorage instance has " +
                    "already been registered with class: " + abilityStorage.getClass());
        }
        abilityStorage = storage;
    }

    public static void registerActivationManager(ActivationManager manager) {
        if (activationManager != null) {
            throw new IllegalStateException("An ActivationManager instance has " +
                    "already been registered with class: " + activationManager.getClass());
        }
        activationManager = manager;
    }

    public static void registerConfigManager(ConfigManager manager) {
        if (configManager != null) {
            throw new IllegalStateException("A ConfigManager instance has " +
                    "already been registered with class: " + configManager.getClass());
        }
        configManager = manager;
    }

    public static void registerLogger(Logger log) {
        if (logger != null) {
            throw new IllegalStateException("A Logger instance has " +
                    "already been registered with class: " + logger.getClass());
        }
        logger = log;
    }

    public static void registerPlugin(ElementalMagicPlugin plugin) {
        if (emPlugin != null) {
            throw new IllegalStateException("A Plugin instance has already " +
                    "been registered with class: " + plugin.getClass());
        }
        emPlugin = plugin;
    }

    public static void registerUserManager(UserManager manager) {
        if (userManager != null) {
            throw new IllegalStateException("A UserManager instance has " +
                    "already been registered with class: " + userManager.getClass());
        }
        userManager = manager;
    }
}
