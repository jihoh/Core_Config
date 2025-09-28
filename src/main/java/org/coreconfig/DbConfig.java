package org.coreconfig;

import jakarta.validation.constraints.*;
import java.time.Duration;

public record DbConfig(
        @NotBlank @Pattern(regexp = "^jdbc:.*") String url,
        @NotBlank String user,
        @NotBlank String password,
        @Positive int poolSize,
        @NotNull Duration timeout
) {}
