package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Main.java — Embedded Grizzly HTTP Server Launcher
 *
 * This class bootstraps the entire Smart Campus API without requiring an
 * external servlet container (e.g., Tomcat/WildFly). It uses the Grizzly
 * HTTP server, which is a lightweight, high-performance NIO framework.
 *
 * Usage:
 *   mvn package
 *   java -jar target/SmartCampusAPI-1.0.0.jar
 *
 * The server will listen on http://localhost:8080/api/v1
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * The base URI where Grizzly will mount the API.
     * This includes the @ApplicationPath value directly so Grizzly
     * correctly routes all requests to /api/v1/...
     *
     * Using the full path here (instead of "/" + relying on @ApplicationPath)
     * is the most reliable approach with Jersey + Grizzly embedded mode.
     */
    public static final String BASE_URI = "http://localhost:8080/api/v1";

    public static void main(String[] args) throws IOException, InterruptedException {
        // Instantiate the JAX-RS Application subclass directly.
        // All registered resources and providers from SmartCampusApplication.getClasses()
        // will be active in this server instance.
        // GrizzlyHttpServerFactory requires a ResourceConfig, so we wrap our Application.
        final ResourceConfig resourceConfig = ResourceConfig.forApplication(new SmartCampusApplication());

        // Create the Grizzly HTTP server and bind it to BASE_URI.
        // All @Path-annotated resources are mounted relative to BASE_URI:
        //   @Path("/rooms")   → http://localhost:8080/api/v1/rooms
        //   @Path("/sensors") → http://localhost:8080/api/v1/sensors
        //   @Path("/")        → http://localhost:8080/api/v1/
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                resourceConfig
        );

        LOGGER.info("==========================================================");
        LOGGER.info("  Smart Campus API started successfully.");
        LOGGER.info("  Discovery Endpoint: " + BASE_URI + "/");
        LOGGER.info("  Rooms Resource:     " + BASE_URI + "/rooms");
        LOGGER.info("  Sensors Resource:   " + BASE_URI + "/sensors");
        LOGGER.info("  Press Ctrl+C to stop the server.");
        LOGGER.info("==========================================================");

        // Register a JVM shutdown hook so the server is gracefully stopped
        // when the process receives SIGTERM or the user presses Ctrl+C.
        // This is more robust than System.in.read() which breaks when stdin is closed.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received. Stopping Smart Campus API...");
            server.shutdown();
            LOGGER.info("Smart Campus API has been shut down cleanly.");
        }));

        // Block the main thread indefinitely until the JVM shuts down.
        Thread.currentThread().join();
    }
}
