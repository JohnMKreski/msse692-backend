package com.arkvalleyevents.msse692_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Ark Valley Events Spring Boot application.
 * Starts the application and opens Swagger UI in the default browser for dev/demo use.
 */
@SpringBootApplication
public class Msse692BackendApplication {

  private static final Logger logger = LoggerFactory.getLogger(Msse692BackendApplication.class);

  public static void main(String[] args) {
    logger.info("Starting Ark Valley Events backend...");
    SpringApplication.run(Msse692BackendApplication.class, args);

    try {
      openSwaggerUI();
    } catch (IOException e) {
      logger.error("Failed to open Swagger UI automatically", e);
    }
  }

  /**
   * Opens Swagger UI in the default browser based on OS.
   * Supports Windows, macOS, and Linux.
   */
  private static void openSwaggerUI() throws IOException {
    String url = "http://localhost:8080/swagger-ui.html";
    Runtime rt = Runtime.getRuntime();
    String os = System.getProperty("os.name").toLowerCase();

    logger.info("Attempting to open Swagger UI at {}", url);

    if (os.contains("win")) {
      rt.exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
    } else if (os.contains("mac")) {
      rt.exec(new String[] { "open", url });
    } else if (os.contains("nix") || os.contains("nux")) {
      rt.exec(new String[] { "xdg-open", url });
    } else {
      logger.warn("Unsupported operating system: {}", os);
      throw new UnsupportedOperationException("Unsupported OS: " + os);
    }
  }
}
