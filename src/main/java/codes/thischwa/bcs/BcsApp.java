package codes.thischwa.bcs;

import codes.thischwa.bcs.service.BirthdayScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * The main application class for the Birthday Calendar Generator (BCG) application.
 *
 * <p>This class acts as the entry point for running the Spring Boot application. It starts the
 * application with a non-web configuration and handles any unexpected exceptions during startup.
 *
 * <p>The application runner bean provided in this class includes special functionality to stop the
 * application after a single invocation of the birthday calendar synchronization process, in case
 * the `run-once` argument is detected.
 */
@ConfigurationPropertiesScan
@SpringBootApplication
@Slf4j
@Profile({"!test", "!backend-test"})
public class BcsApp {

  /**
   * The main method serves as the entry point of the Birthday Calendar Generator (BCG) application.
   * It configures and starts the Spring Boot application in non-web mode. If an unexpected exception
   * occurs during the startup process, it logs the error and terminates the application with a
   * specific exit code.
   *
   * @param args an array of command-line arguments passed to the application, which can be used to
   *             customize the application's behavior at runtime.
   */
  public static void main(String[] args) {
    try {
      new SpringApplicationBuilder(BcsApp.class).web(WebApplicationType.NONE).run(args);
    } catch (Exception e) {
      log.error("Unexpected exception, Spring Boot stops! Message: {}", e.getMessage());
      System.exit(10);
    }
  }

  /**
   * Adds a command-line option to stop the application after calendar synchronization.
   */
  @Bean
  public ApplicationRunner applicationRunner(BirthdayScheduler birthdayScheduler) {
    return args -> {
      if (args.containsOption("run-once")) {
        log.info("Argument '--run-once' detected. Starting calendar synchronization...");
        birthdayScheduler.process(); // Trigger calendar sync
        log.info("Calendar synchronization complete (run-once). Shutting down application.");
        System.exit(0);
      }
    };
  }
}