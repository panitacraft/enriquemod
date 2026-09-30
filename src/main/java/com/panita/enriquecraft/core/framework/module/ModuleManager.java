package com.panita.enriquecraft.core.framework.module;

import com.panita.enriquecraft.core.framework.command.CommandCatalog;
import com.panita.enriquecraft.core.framework.command.CommandRegistry;
import com.panita.enriquecraft.core.framework.command.CommandTreeBuilder;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.core.framework.scan.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Loads modules: registers their services, then discovers and builds their listeners and commands.
 * Modules are loaded in the order they are registered, so later modules can use earlier services.
 */
public final class ModuleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleManager.class);

    private final ServiceRegistry services = new ServiceRegistry();
    private final CommandCatalog catalog = new CommandCatalog();
    private final ClassScanner scanner;

    public ModuleManager(String modId) {
        this.scanner = new ClassScanner(modId);
        services.register(CommandCatalog.class, catalog);
        new CommandRegistry(catalog, new CommandTreeBuilder()).register();
    }

    public void register(EnriquecraftModule module) {
        module.registerServices(services);

        List<Class<? extends ModListener>> listeners = scanner.scan(module.listenerPackage(), ModListener.class);
        listeners.forEach(type -> services.instantiate(type).register());

        List<Class<? extends ModCommand>> commands = scanner.scan(module.commandPackage(), ModCommand.class);
        commands.forEach(type -> catalog.register(services.instantiate(type)));

        LOGGER.info("Loaded module {} with {} listener(s) and {} command(s)",
                module.getClass().getSimpleName(), listeners.size(), commands.size());
    }
}
