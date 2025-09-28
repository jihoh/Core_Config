package org.coreconfig;

/**
 * A single, type-safe record that defines the entire configuration schema for the application.
 * The name of each component (e.g., 'http', 'db') corresponds to a top-level
 * block in the application.conf file.
 */
public record AppConfig(
        HttpConfig http,
        DbConfig db
) {}
