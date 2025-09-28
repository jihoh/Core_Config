package org.coreconfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

/**
 * A simple, fluent, and production-ready entry point for loading configuration.
 *
 * This utility loads, maps, and validates the entire configuration graph in a single,
 * static method call at application startup.
 */
public final class SimpleConfig {

    private static final ConfigMapper MAPPER = new ConfigMapper();

    private SimpleConfig() {}

    /**
     * Boots the entire configuration system once.
     *
     * It loads the root config, accounting for environment profiles (e.g., -Dconfig.profile=prod),
     * then maps the contents to the structure of the provided aggregate record class.
     *
     * @param aggregateConfigType The main record class that defines the config structure (e.g., AppConfig.class).
     * @return A fully populated and validated instance of the aggregate config record.
     * @throws RuntimeException if loading, mapping, or validation fails.
     */
    public static <T> T boot(Class<T> aggregateConfigType) {
        try {
            // 1. Load the raw configuration, applying any environment profile.
            Config rootConfig = loadWithProfile();
            ConfigLoader.logEffectiveConfig(rootConfig); // Always log for debuggability.

            // 2. Use reflection to build the aggregate record.
            RecordComponent[] components = aggregateConfigType.getRecordComponents();
            Object[] constructorArgs = new Object[components.length];
            Class<?>[] paramTypes = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new);

            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                String path = rc.getName(); // Convention: record component name == config path
                constructorArgs[i] = MAPPER.map(rootConfig, path, rc.getType());
            }

            // 3. Create and return the final, validated config instance.
            Constructor<T> constructor = aggregateConfigType.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(constructorArgs);

        } catch (Exception e) {
            // Wrap any exception in a clear, top-level error.
            System.err.println("FATAL: Application configuration failed to boot.");
            throw new RuntimeException("Could not initialize configuration for " + aggregateConfigType.getSimpleName(), e);
        }
    }

    /**
     * Internal helper to load configuration based on an optional profile.
     */
    private static Config loadWithProfile() {
        // Check for a profile in system properties or environment variables.
        String profile = System.getProperty("config.profile", System.getenv("CONFIG_PROFILE"));

        Config baseConfig = ConfigFactory.load(); // Loads application.conf

        if (profile != null && !profile.isBlank()) {
            System.out.println("INFO: Activating configuration profile: " + profile);
            // Loads application-{profile}.conf and layers it on top of application.conf
            return ConfigFactory.load("application-" + profile)
                    .withFallback(baseConfig);
        }

        // The enhanced ConfigLoader resolves secrets and system properties.
        return ConfigLoader.load(baseConfig);
    }
}

