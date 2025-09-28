package org.coreconfig;


public class Application {

    public static void main(String[] args) {
        try {
            // ONE line to load, map, validate, and log the entire configuration.
            AppConfig appConfig = SimpleConfig.boot(AppConfig.class);

            // Now, use the final, type-safe, and validated config object.
            // Pass it to your services using Dependency Injection.
            System.out.println("SUCCESS: Configuration loaded successfully.");
            System.out.println("Starting web server on port: " + appConfig.http().port());
            System.out.println("Connecting to database user: " + appConfig.db().user());

            // Example of using the config to start services:
            // DatabaseService dbService = new DatabaseService(appConfig.db());
            // WebServer webServer = new WebServer(appConfig.http());
            // webServer.start();

        } catch (Exception e) {
            // The boot process will throw a detailed exception on any failure.
            System.err.println("FATAL: Application failed to start due to a configuration error.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

