package com.panita.enriquecraft.core.framework.scan;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Finds the classes of this mod inside a package, both in a development build (directories) and
 * in the packaged jar. Classes are loaded without being initialized.
 */
public final class ClassScanner {

    private static final String CLASS_SUFFIX = ".class";

    private final ModContainer mod;
    private final ClassLoader classLoader = ClassScanner.class.getClassLoader();

    public ClassScanner(String modId) {
        this.mod = FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("Mod container not found: " + modId));
    }

    /**
     * Lists the concrete top-level classes in a package and its subpackages that extend or
     * implement the given type, sorted by class name.
     */
    public <T> List<Class<? extends T>> scan(String packageName, Class<T> supertype) {
        return classNames(packageName).stream()
                .map(this::load)
                .filter(type -> supertype.isAssignableFrom(type) && isConcrete(type))
                .<Class<? extends T>>map(type -> type.asSubclass(supertype))
                .toList();
    }

    private Set<String> classNames(String packageName) {
        String relativeDirectory = packageName.replace('.', '/');
        Set<String> names = new TreeSet<>();
        for (Path root : mod.getRootPaths()) {
            Path directory = root.resolve(relativeDirectory);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(ClassScanner::isTopLevelClassFile)
                        .map(file -> toClassName(root, file))
                        .forEach(names::add);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not scan package " + packageName, e);
            }
        }
        return names;
    }

    private static boolean isTopLevelClassFile(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(CLASS_SUFFIX)
                && !name.contains("$")
                && !name.equals("package-info.class")
                && !name.equals("module-info.class");
    }

    private static String toClassName(Path root, Path file) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - CLASS_SUFFIX.length()).replace('/', '.');
    }

    private Class<?> load(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load scanned class " + className, e);
        }
    }

    private static boolean isConcrete(Class<?> type) {
        return !type.isInterface() && !Modifier.isAbstract(type.getModifiers());
    }
}
