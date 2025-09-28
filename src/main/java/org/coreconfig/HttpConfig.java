package org.coreconfig;

import jakarta.validation.constraints.*;
import java.time.Duration;

public record HttpConfig(
        @Min(1) @Max(65535) int port,
        @NotBlank String host,
        @NotNull Duration idleTimeout
) {}
