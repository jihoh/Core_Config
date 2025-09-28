Project Structure and Environment Configuration GuideThis guide outlines the recommended project structure for a multi-service repository and the standard practice for managing environment-specific configurations (dev, qa, prod).1. Recommended Project StructureFor a repository with many services, a multi-module Maven project is the ideal structure. This allows you to centralize shared code (like our configuration library) while keeping each service isolated in its own module.Directory LayoutThe project should be organized with a parent pom.xml at the root, a core-config module for the shared library, and a separate module for each application.your-monorepo/
├── pom.xml                   (Parent POM managing all modules and dependencies)
│
├── core-config/              (The shared configuration library module)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/yourorg/coreconfig/
│       │   │   ├── ... (SimpleConfig, HttpConfig, etc.)
│       │   └── resources/
│       │       ├── logback.xml
│       │       ├── db-dev.conf       (NEW: Shared dev DB config)
│       │       ├── db-qa.conf        (NEW: Shared qa DB config)
│       │       └── db-prod.conf      (NEW: Shared prod DB config)
│       └── test/
│           └── ...
│
├── trading-engine/           (Example application module #1)
│   ├── pom.xml               (Depends on core-config)
│   └── src/
│       ├── main/
│       │   ├── java/com/yourorg/trading/
│       │   │   └── ... (TradingEngineMain, RiskConfig, etc.)
│       │   └── resources/
│       │       ├── application.conf         (Base and local dev config)
│       │       ├── application-dev.conf
│       │       ├── application-qa.conf
│       │       └── application-prod.conf
│       └── test/
│           └── ...
│
└── ... (other services)
Key ConceptsParent pom.xml: Manages shared dependency versions for all modules, ensuring consistency.core-config Module: This is your reusable library. It contains all the shared logic, common configuration records (HttpConfig, DbConfig), and now, shared environment configuration files.Application Modules: Each service (trading-engine, reporting-service) is a self-contained application. It defines its unique configuration needs and composes them with the shared ones from core-config.2. Managing dev, qa, and prod EnvironmentsThe configuration library is designed to handle multiple environments using a layering and composition approach.Step 1: Create the Base application.confThis file, located in src/main/resources of your service, should contain the complete configuration structure with safe defaults for local development.Example: trading-engine/src/main/resources/application.conf# This is the base configuration, used by default for local development.
http {
host = "0.0.0.0", port = 8080, idle-timeout = 60s
}
db {
host = "localhost", port = 5432, user = "developer", password = "dev-password", pool-size = 10, timeout = 5s
}
risk {
max-order-size = 1000, risk-model = "SimpleValidation"
}
Step 2: Centralize Shared Environment Config (Best Practice)To avoid duplicating common settings (like database hosts for QA) across 100+ services, place them in the core-config module.Example: core-config/src/main/resources/db-qa.conf# This file contains ONLY the shared QA database configuration.
# It will be included by any service that needs to connect to the QA DB.
db {
host = "db.qa.yourorg.com"
user = ${?QA_DB_USER}       # Read from environment variable
password = ${?QA_DB_PASSWORD}
}
Step 3: Create Profile-Specific Override Files using includeNow, your service's override files become much simpler. They just need to include the shared configuration and then add any service-specific overrides.Example: trading-engine/src/main/resources/application-qa.conf# Overrides for the QA environment for the Trading Engine service.

# 1. Pull in the shared QA database configuration from the core-config library.
include "db-qa.conf"

# 2. Add any overrides that are SPECIFIC to this service in QA.
risk {
risk-model = "ValueAtRisk"
}
The include directive tells the config loader to find db-qa.conf on the classpath and merge its contents. Since core-config is a dependency, its resource files are on the classpath.Step 4: Activate a Profile at RuntimeThis step remains the same. You activate a profile by setting a Java system property. The library handles the rest, including resolving the include statements.Running in QA:# Merges application-qa.conf, which in turn includes db-qa.conf
java -Dconfig.profile=qa -jar trading-engine.jar
