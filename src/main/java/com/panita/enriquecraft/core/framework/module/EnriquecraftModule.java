package com.panita.enriquecraft.core.framework.module;

import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;

/**
 * A self-contained feature set of the mod, living in its own package. Commands in its
 * {@code commands} subpackage and listeners in its {@code listeners} subpackage are discovered
 * automatically.
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
}
