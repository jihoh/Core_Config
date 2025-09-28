package org.coreconfig;

import com.typesafe.config.Config;
import jakarta.validation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Set;

/**
 * A generic, reusable binder that maps a Typesafe Config path to an immutable Java Record.
 * It automatically validates the resulting record instance using jakarta.validation annotations.
 */
public final class ConfigMapper {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Maps a configuration path to a record of the specified type.
     *
     * @param root The root configuration object.
     * @param path The path to the config slice (e.g., "http", "db").
     * @param recordType The .class of the record to map to.
     * @return A validated, populated instance of the record.
     * @throws ConfigValidationException if validation fails.
     * @throws RuntimeException for mapping errors.
     */
    public <T> T map(Config root, String path, Class<T> recordType) {
        if (!root.hasPath(path)) {
            throw new IllegalArgumentException("Configuration path not found: " + path);
        }
        Config slice = root.getConfig(path);

        try {
            RecordComponent[] components = recordType.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] paramTypes = new Class<?>[components.length];

            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                paramTypes[i] = rc.getType();
                args[i] = getValue(slice, rc.getName(), rc.getType());
            }

            Constructor<T> constructor = recordType.getDeclaredConstructor(paramTypes);
            T instance = constructor.newInstance(args);

            validate(instance);
            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to map config path '" + path + "' to " + recordType.getSimpleName(), e);
        }
    }

    private Object getValue(Config config, String name, Class<?> type) {
        // Convert camelCase record name to kebab-case config key
        String key = name.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
        if (!config.hasPath(key)) {
            throw new IllegalStateException("Missing required config key: " + key + " in path " + config.origin().description());
        }
        if (type == int.class) return config.getInt(key);
        if (type == String.class) return config.getString(key);
        if (type == boolean.class) return config.getBoolean(key);
        if (type == Duration.class) return config.getDuration(key);
        if (type == double.class) return config.getDouble(key);
        if (type == long.class) return config.getLong(key);

        throw new UnsupportedOperationException("Unsupported config type: " + type.getSimpleName());
    }

    private <T> void validate(T bean) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(bean);
        if (!violations.isEmpty()) {
            throw new ConfigValidationException(violations);
        }
    }
}
