package org.coreconfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ConfigMapper Tests")
class ConfigMapperTest {

    private ConfigMapper mapper;

    @BeforeEach
    void setUp() {
        // A new mapper is created for each test to ensure isolation.
        this.mapper = new ConfigMapper();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessTests {

        @Test
        @DisplayName("Should map a valid HOCON block to HttpConfig record")
        void map_withValidHttpConfig_shouldSucceed() {
            // Arrange
            String hocon = """
                http {
                  host = "localhost"
                  port = 8080
                  idle-timeout = 60s
                }
            """;
            Config config = ConfigFactory.parseString(hocon);

            // Act
            HttpConfig httpConfig = mapper.map(config, "http", HttpConfig.class);

            // Assert
            assertThat(httpConfig).isNotNull();
            assertThat(httpConfig.host()).isEqualTo("localhost");
            assertThat(httpConfig.port()).isEqualTo(8080);
            assertThat(httpConfig.idleTimeout()).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        @DisplayName("Should map a valid HOCON block to DbConfig record")
        void map_withValidDbConfig_shouldSucceed() {
            // Arrange
            String hocon = """
                db {
                  url = "jdbc:postgresql://localhost:5432/test"
                  user = "testuser"
                  password = "testpassword"
                  pool-size = 10
                  timeout = 5s
                }
            """;
            Config config = ConfigFactory.parseString(hocon);

            // Act
            DbConfig dbConfig = mapper.map(config, "db", DbConfig.class);

            // Assert
            assertThat(dbConfig).isNotNull();
            assertThat(dbConfig.url()).isEqualTo("jdbc:postgresql://localhost:5432/test");
            assertThat(dbConfig.user()).isEqualTo("testuser");
            assertThat(dbConfig.password()).isEqualTo("testpassword");
            assertThat(dbConfig.poolSize()).isEqualTo(10);
            assertThat(dbConfig.timeout()).isEqualTo(Duration.ofSeconds(5));
        }
    }

    @Nested
    @DisplayName("Failure Scenarios - Validation")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should throw when port is out of range")
        void map_withInvalidPort_shouldThrowValidationException() {
            // Arrange
            String hocon = "http { host = \"localhost\", port = 99999, idle-timeout = 10s }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "http", HttpConfig.class))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("port must be less than or equal to 65535");
        }

        @Test
        @DisplayName("Should throw when host is blank")
        void map_withBlankHost_shouldThrowValidationException() {
            // Arrange
            String hocon = "http { host = \" \", port = 8080, idle-timeout = 10s }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "http", HttpConfig.class))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("host must not be blank");
        }

        @Test
        @DisplayName("Should throw when DB URL pattern is incorrect")
        void map_withInvalidDbUrl_shouldThrowValidationException() {
            // Arrange
            String hocon = "db { url = \"http://invalid\", user=\"a\", password=\"b\", pool-size=1, timeout=1s }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "db", DbConfig.class))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("url must match \"^jdbc:.*\"");
        }

        @Test
        @DisplayName("Should throw when pool size is not positive")
        void map_withNonPositivePoolSize_shouldThrowValidationException() {
            // Arrange
            String hocon = "db { url = \"jdbc:h2:mem:\", user=\"a\", password=\"b\", pool-size=0, timeout=1s }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "db", DbConfig.class))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("poolSize must be positive");
        }
    }

    @Nested
    @DisplayName("Failure Scenarios - Parsing and Mapping")
    class MappingFailureTests {

        @Test
        @DisplayName("Should throw when a required key is missing")
        void map_withMissingRequiredKey_shouldThrow() {
            // Arrange: 'idle-timeout' is missing
            String hocon = "http { host = \"localhost\", port = 8080 }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "http", HttpConfig.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessageContaining("Missing required config key: idle-timeout");
        }

        @Test
        @DisplayName("Should throw when the entire config path is missing")
        void map_withMissingPath_shouldThrow() {
            // Arrange: 'http' path does not exist
            String hocon = "database { url = \"jdbc:...\" }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "http", HttpConfig.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Configuration path not found: http");
        }

        @Test
        @DisplayName("Should throw when a value has the wrong type")
        void map_withWrongValueType_shouldThrow() {
            // Arrange: 'port' is a string instead of a number
            String hocon = "http { host = \"localhost\", port = \"not-a-port\", idle-timeout = 10s }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert: The Typesafe Config library will throw a specific exception
            assertThatThrownBy(() -> mapper.map(config, "http", HttpConfig.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to map config path 'http' to HttpConfig")
                    .getRootCause()
                    .isInstanceOf(com.typesafe.config.ConfigException.WrongType.class);
        }

        // A record with a type not supported by our getValue() method.
        private record UnsupportedTypeConfig(java.util.List<String> items) {}

        @Test
        @DisplayName("Should throw when a record contains an unsupported type")
        void map_withUnsupportedType_shouldThrow() {
            // Arrange
            String hocon = "unsupported { items = [\"a\", \"b\"] }";
            Config config = ConfigFactory.parseString(hocon);

            // Act & Assert
            assertThatThrownBy(() -> mapper.map(config, "unsupported", UnsupportedTypeConfig.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                    .hasRootCauseMessage("Unsupported config type: List");
        }
    }
}
