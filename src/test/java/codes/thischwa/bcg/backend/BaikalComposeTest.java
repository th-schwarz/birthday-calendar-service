package codes.thischwa.bcg.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class BaikalComposeTest extends AbstractBackendTest {

  private static final String COMPOSE_DIR = System.getProperty("user.dir") + "/src/docker/baikal";
  private static String BASE_URL;

  private static final int SERVICE_PORT = 80;
  private static final String SERVICE_NAME = "baikal-it";

  @Container
  public static ComposeContainer baikal = new ComposeContainer(
      new File(COMPOSE_DIR + "/docker-compose.yml"))
      .withLocalCompose(true)
      .withExposedService(SERVICE_NAME, SERVICE_PORT, Wait.forHttp("/")
          .forStatusCode(HttpStatus.SC_OK)
          .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SEC)));

  @BeforeAll
  public static void testIsRunning() throws IOException {
    String host = baikal.getServiceHost(SERVICE_NAME, SERVICE_PORT);
    Integer port = baikal.getServicePort(SERVICE_NAME, SERVICE_PORT);
    BASE_URL = "http://" + host + ":" + port + "/";
    log.info("Base URL: {}", BASE_URL);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertEquals(HttpStatus.SC_OK, responseCode, "Baikal should return HTTP 200");
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "dav.php/addressbooks/dev-user/default/");
    registry.add("dav.cal-url", () -> BASE_URL + "dav.php/calendars/dev-user/default/");
  }

  @Test
  @DisplayName("BAIKAL end-to-end test (docker-compose)")
  void completeProcess() throws Exception {
    log.info("Starting BAIKAL end-to-end test using docker-compose CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
