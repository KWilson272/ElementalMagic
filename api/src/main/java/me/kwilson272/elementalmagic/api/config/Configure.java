package me.kwilson272.elementalmagic.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to link fields to values in a configuration file.<p>
 *
 * <strong>Usage Note:</strong> fields marked with this annotation must have a
 * default value, ex: {@code private int damage = 2.0}, which will be used as
 * the default value in the configuration file. Additionally, fields marked
 * should be processable using the standard get/set methods in
 * {@link org.bukkit.configuration.file.FileConfiguration}, or more
 * specifically: {@link org.bukkit.configuration.MemorySection}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Configure {

    /**
     * @return the complete {@link String} path to the config value
     */
    String path();

    /**
     * @return the {@link Config} the config value is in
     */
    Config config();
}

