package me.kwilson272.elementalmagic.core.config;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.config.Config;
import me.kwilson272.elementalmagic.api.config.ConfigManager;
import me.kwilson272.elementalmagic.api.config.Configure;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManagerImpl implements ConfigManager {

    private final Map<Config, ConfigFileLink> configs;

    public ConfigManagerImpl() {
        this.configs = new HashMap<>();
    }

    @Override
    public void enable() {
        ElementalMagicApi.plugin().getDataFolder().mkdirs();
        loadConfig(Config.ABILITIES, "abilities.yml");
        loadConfig(Config.LANGUAGE, "language.yml");
        loadConfig(Config.PLUGIN_PROPERTIES, "properties.yml");
    }

    private void loadConfig(Config config, String fileName) {
        String dataPath = ElementalMagicApi.plugin().getDataFolder().getPath();
        File file = new File(dataPath + File.separator + fileName);
        
        makeFile(file);
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
        configs.put(config, new ConfigFileLink(fileConfig, file));
    }

    private void makeFile(File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            String error = "Could not create file: " + file.getPath();
            ElementalMagicApi.logger().log(Level.SEVERE, error);
            e.printStackTrace();
        }
    }

    @Override
    public void disable(boolean shutDown) {
        save(Config.ABILITIES);
        save(Config.LANGUAGE);
        save(Config.PLUGIN_PROPERTIES);
        configs.clear();
    }

    @Override
    public FileConfiguration get(Config config) {
        return configs.get(config).configuration;
    }

    @Override
    public void save(Config config) {
        ConfigFileLink link = configs.get(config);
        FileConfiguration fConfig = link.configuration;
        File file = link.file;

        fConfig.options().copyDefaults(true);
        try {
            fConfig.save(file);
        } catch (IOException e) {
            String error = "Could not save file: " + file.getPath();
            ElementalMagicApi.logger().log(Level.SEVERE, error);
            e.printStackTrace();
        }
    }

    @Override
    public void configure(Object object) {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Configure.class)) {
                continue;
            }

            Configure options = field.getAnnotation(Configure.class);
            FileConfiguration config = get(options.config());
            String path = options.path();

            field.setAccessible(true);
            try {
                if (!config.contains(path)) {
                    config.addDefault(path, field.get(object));
                } else {
                    field.set(object, config.get(path));
                }
            } catch (IllegalAccessException e) {
                String fName = field.getName();
                String cName = object.getClass().getName();
                String err = "Unable to configure '" + fName + "' in '" + cName + "'.";
                ElementalMagicApi.logger().log(Level.WARNING, err);
                e.printStackTrace();
            }
        }
    }

    public record ConfigFileLink(FileConfiguration configuration, File file) {}
}

