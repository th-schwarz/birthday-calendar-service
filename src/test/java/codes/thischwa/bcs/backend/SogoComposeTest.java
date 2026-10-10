package codes.thischwa.bcs.backend;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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
public class SogoComposeTest extends AbstractBackendTest {

  private static String BASE_URL;
  private static final int SERVICE_PORT = 80;
  private static final String SERVICE_NAME = "nginx";
  private static final String CARD_PATH = "/SOGo/dav/dev-user/Contacts/personal/";
  private static final String DAV_USER_CREDENTIALS = "dev-user:test123";

  private static final DockerComposeConfig dockerConfig;

  static {
    try {
      dockerConfig = prepareDockerCompose("sogo", "sogo");
    } catch (IOException e) {
      throw new RuntimeException("Failed to prepare docker-compose for SOGO test", e);
    }
  }

  @Container
  static ComposeContainer sogo = new ComposeContainer(dockerConfig.composeFile().toFile())
      .withPull(true)
      .withEnv("COMPOSE_PROJECT_DIRECTORY", dockerConfig.workingDir().toString())
      .withExposedService(
          SERVICE_NAME,
          SERVICE_PORT,
          Wait.forHttp("/SOGo/")
              .forPort(SERVICE_PORT)
              .forStatusCodeMatching(status -> status >= HttpStatus.SC_OK && status < 500)
              .withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SEC))
      );

  @BeforeAll
  static void testIsRunning() throws IOException, InterruptedException {
    String host = sogo.getServiceHost(SERVICE_NAME, SERVICE_PORT);
    Integer port = sogo.getServicePort(SERVICE_NAME, SERVICE_PORT);
    BASE_URL = "http://" + host + ":" + port + "/";
    log.info("Base URL: {}", BASE_URL);

    // The web UI answers long before sogod can serve DAV requests (it returns 503 until its
    // workers and the database are ready), so wait for the real CardDAV endpoint.
    awaitDavReady();
  }

  private static void awaitDavReady() throws IOException, InterruptedException {
    String auth = Base64.getEncoder().encodeToString(DAV_USER_CREDENTIALS.getBytes(StandardCharsets.UTF_8));
    HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + CARD_PATH.substring(1)))
        .method("PROPFIND", HttpRequest.BodyPublishers.noBody())
        .header("Authorization", "Basic " + auth)
        .header("Depth", "0")
        .timeout(Duration.ofSeconds(10))
        .build();
    HttpClient client = HttpClient.newHttpClient();
    Instant deadline = Instant.now().plus(Duration.ofSeconds(STARTUP_TIMEOUT_SEC));
    int status = -1;
    while (Instant.now().isBefore(deadline)) {
      try {
        status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status == HttpStatus.SC_MULTI_STATUS) {
          log.info("SOGo CardDAV endpoint is ready");
          return;
        }
        log.info("SOGo CardDAV endpoint not ready yet (status {}), retrying ...", status);
      } catch (IOException e) {
        log.info("SOGo CardDAV endpoint not reachable yet ({}), retrying ...", e.toString());
      }
      Thread.sleep(2000);
    }
    fail("SOGo CardDAV endpoint did not become ready in time. Last status: " + status);
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // These credentials are aligned with src/docker/sogo/config/init-users.sql
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.base-url", () -> BASE_URL.replaceAll("/$", ""));
    registry.add("dav.card-paths[0]", () -> CARD_PATH);
    registry.add("dav.cal-path", () -> "/SOGo/dav/dev-user/Calendar/personal/");
  }

  @Test
  @DisplayName("SOGO end-to-end test (docker-compose)")
  void completeProcess() throws Exception {
    log.info("Starting SOGO end-to-end test using docker-compose CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
