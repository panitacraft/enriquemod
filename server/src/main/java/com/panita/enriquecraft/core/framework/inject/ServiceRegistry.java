package com.panita.enriquecraft.core.framework.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the services of the mod and builds classes by constructor injection: every parameter of
 * the constructor is resolved by its exact type from the registered services.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> type, T instance) {
        if (services.putIfAbsent(type, instance) != null) {
            throw new IllegalStateException("A service is already registered for " + type.getName());
        }
    }

    public <T> T get(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("No service is registered for " + type.getName());
        }
        return type.cast(service);
    }

    /**
     * Creates an instance through the class's only public constructor.
     *
     * @param type the class to build; it must declare exactly one public constructor
     * @throws IllegalStateException if the constructor is ambiguous, a parameter has no service, or construction fails
     */
    public <T> T instantiate(Class<T> type) {
        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException(type.getName() + " must declare exactly one public constructor");
        }
        Constructor<?> constructor = constructors[0];
        Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(parameterType -> resolve(type, parameterType))
                .toArray();
        try {
            return type.cast(constructor.newInstance(arguments));
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Constructor of " + type.getName() + " failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate " + type.getName(), e);
        }
    }

    private Object resolve(Class<?> owner, Class<?> parameterType) {
        Object service = services.get(parameterType);
        if (service == null) {
            throw new IllegalStateException(owner.getName() + " needs a " + parameterType.getName()
                    + ", but no such service is registered");
        }
        return service;
    }
}
