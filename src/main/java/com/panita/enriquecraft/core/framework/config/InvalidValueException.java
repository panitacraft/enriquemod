package com.panita.enriquecraft.core.framework.config;

/**
 * Thrown when a raw config value cannot be used. The message states the rule that was broken,
 * for example {@code must be true or false}.
 */
final class InvalidValueException extends RuntimeException {

    InvalidValueException(String message) {
        super(message, null, false, false);
    }
}
