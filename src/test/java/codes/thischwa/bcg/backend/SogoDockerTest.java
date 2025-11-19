package codes.thischwa.bcg.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
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
public class SogoDockerTest extends AbstractBackendTest {

  private static String BASE_URL;

  @Container
  public static ComposeContainer sogoEnvironment = new ComposeContainer(
      new File("src/docker/sogo/docker-compose-test.yml"))
      .withExposedService("nginx", 80, Wait.forHttp("/SOGo/")
          .forStatusCode(200)
          .withStartupTimeout(Duration.ofSeconds(60)))
      .withLocalCompose(true);

  @BeforeAll
  public static void testIsRunning() throws IOException {
    String host = sogoEnvironment.getServiceHost("nginx", 80);
    Integer port = sogoEnvironment.getServicePort("nginx", 80);
    BASE_URL = "http://" + host + ":" + port + "/";
    log.info("Base URL: {}", BASE_URL);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertEquals(200, responseCode, "SOGo should return HTTP 200");
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "SOGo/dav/dev-user/Contacts/personal/");
    registry.add("dav.cal-url", () -> BASE_URL + "SOGo/dav/dev-user/Calendar/personal/");
  }

  @Test
  @DisplayName("SOGo end-to-end test")
  void completeProcess() throws Exception {
    log.info("Starting SOGo end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
