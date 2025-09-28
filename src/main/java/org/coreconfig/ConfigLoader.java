package org.coreconfig;

import com.typesafe.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal utility for resolving secrets and layering system properties.
 */
public final class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {}

    /**
     * Takes a base config (e.g., from file) and layers secrets and system properties on top.
     */
    public static Config load(Config baseConfig) {
        Config secretsConfig = resolveSecretsFromFiles(baseConfig);

        // Final resolution order: System Properties > Secrets > Loaded Files
        return ConfigFactory.systemProperties()
                .withFallback(secretsConfig)
                .withFallback(baseConfig)
                .resolve();
    }

    /**
     * Logs the effective configuration, redacting any keys that appear to be secrets.
     */
    public static void logEffectiveConfig(Config config) {
        if (!log.isInfoEnabled()) {
            return;
        }
        ConfigRenderOptions renderOpts = ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setJson(false);

        // Redact any key containing "password", "secret", or "token" for security.
        Config redactedConfig = config.withOnlyPath("root").withValue("root",
                ConfigValueFactory.fromMap(
                        config.root().unwrapped().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> {
                                            String key = entry.getKey().toLowerCase();
                                            if (key.contains("password") || key.contains("secret") || key.contains("token")) {
                                                return "[REDACTED]";
                                            }
                                            return entry.getValue();
                                        }
                                ))
                )
        );

        log.info("Effective application configuration:\n---\n{}---", redactedConfig.root().render(renderOpts));
    }

    /**
     * Scans environment variables for keys ending in _FILE and replaces them with file content.
     */
    private static Config resolveSecretsFromFiles(Config config) {
        Map<String, String> fileSecrets = System.getenv().entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_FILE"))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().replace("_FILE", ""), // e.g., DB_PASSWORD_FILE -> DB_PASSWORD
                        entry -> {
                            try {
                                return new String(Files.readAllBytes(Paths.get(entry.getValue()))).trim();
                            } catch (IOException e) {
                                log.error("Failed to read secret from file: {}", entry.getValue(), e);
                                return "";
                            }
                        }
                ));

        if (fileSecrets.isEmpty()) {
            return ConfigFactory.empty();
        }

        log.info("Resolved {} secrets from file paths.", fileSecrets.size());
        return ConfigFactory.parseMap(fileSecrets);
    }
}

