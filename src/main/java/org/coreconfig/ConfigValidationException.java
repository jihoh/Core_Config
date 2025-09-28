package org.coreconfig;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigValidationException extends RuntimeException {
    public ConfigValidationException(Set<ConstraintViolation<?>> violations) {
        super(formatMessage(violations));
    }

    private static String formatMessage(Set<ConstraintViolation<?>> violations) {
        return "Config validation failed: " + violations.stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
    }
}
