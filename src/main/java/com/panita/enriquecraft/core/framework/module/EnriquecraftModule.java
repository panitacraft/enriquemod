package com.panita.enriquecraft.core.framework.module;

import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;

/**
 * A self-contained feature set of the mod, living in its own package. Commands in its
 * {@code commands} subpackage, listeners in its {@code listeners} subpackage and its config class in
 * its {@code config} subpackage are discovered automatically.
 */
public interface EnriquecraftModule {

    /**
     * Creates the module's services and makes them available to its commands and listeners, and to
     * modules registered after it.
     */
    void registerServices(ServiceRegistry services);

    /** The package that holds the module's classes; by default, the package of the module class. */
    default String basePackage() {
        return getClass().getPackageName();
    }

    default String commandPackage() {
        return basePackage() + ".commands";
    }

    default String listenerPackage() {
        return basePackage() + ".listeners";
    }

    default String configPackage() {
        return basePackage() + ".config";
    }

    /** The top-level key of this module in the config file; by default, the last segment of the package name. */
    default String configSection() {
        String basePackage = basePackage();
        return basePackage.substring(basePackage.lastIndexOf('.') + 1);
    }
}
