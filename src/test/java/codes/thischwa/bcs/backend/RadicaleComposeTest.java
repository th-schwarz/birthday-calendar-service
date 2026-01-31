package codes.thischwa.bcs.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class RadicaleComposeTest extends AbstractBackendTest {

  private static String BASE_URL;
  private static final int SERVICE_PORT = 5232;
  private static final String SERVICE_NAME = "radicale-it";

  @Container
  public static ComposeContainer radicale = new ComposeContainer(
      new File("src/docker/radicale/docker-compose.yml"))
      .withLocalCompose(true)
      .withPull(true)
      //.withLogConsumer(SERVICE_NAME, new Slf4jLogConsumer(log).withPrefix(SERVICE_NAME))
      .withExposedService(
          SERVICE_NAME,
          SERVICE_PORT,
          Wait.forHttp("/")
              .forStatusCodeMatching(status -> status >= HttpStatus.SC_OK && status < 500)
              .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SEC))
      );

  @BeforeAll
  public static void testIsRunning() throws IOException {
    String host = radicale.getServiceHost(SERVICE_NAME, SERVICE_PORT);
    Integer port = radicale.getServicePort(SERVICE_NAME, SERVICE_PORT);
    BASE_URL = "http://" + host + ":" + port + "/";
    log.info("Base URL: {}", BASE_URL);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertTrue(responseCode >= 200 && responseCode < 500,
        "Radicale should be reachable over HTTP (2xx/3xx/4xx). Actual: " + responseCode);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // These credentials and collection IDs are aligned with src/docker/radicale/config and data
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    // CardDAV (address book) and CalDAV (calendar) collections are pre-seeded in test data
    registry.add("dav.card-url", () -> BASE_URL + "dev-user/d8d512ec-7535-840c-7141-00bc758d6296/");
    registry.add("dav.cal-url", () -> BASE_URL + "dev-user/c028244a-3c9c-d304-5e1e-2f80761d4e66/");
  }

  @Test
  @DisplayName("RADICALE end-to-end test (docker-compose)")
  void completeProcess() throws Exception {
    log.info("Starting RADICALE end-to-end test using docker-compose CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
