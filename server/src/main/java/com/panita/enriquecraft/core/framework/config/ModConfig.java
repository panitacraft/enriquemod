package com.panita.enriquecraft.core.framework.config;

/**
 * The config section of a module. Implementations inside a module's {@code config} package are
 * discovered automatically. A config class declares one public constructor that takes a
 * {@link ConfigSectionBuilder} and exposes its {@link ConfigValue}s as public final fields.
 * Other classes receive the config object by constructor injection.
 */
public interface ModConfig {
}
