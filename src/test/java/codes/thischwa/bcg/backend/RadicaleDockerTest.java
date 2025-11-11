package codes.thischwa.bcg.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class RadicaleDockerTest extends AbstractBackendTest {

  private static String BASE_URL;

  // Radicale runs on port 5232 in the container
  @Container
  public static GenericContainer<?> radicale = new GenericContainer<>("thschwarz/radicale-it:1.0")
      .withExposedPorts(5232)
      .withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64"))
      .waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(10)));

  @BeforeAll
  public static void testRadîcaleIsRunning() throws IOException {
    BASE_URL = "http://" + radicale.getHost() + ":" + radicale.getMappedPort(5232) + "/";
    log.info("Base URL: {}", BASE_URL);

    // simple HTTP-Check
    HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL).openConnection();
    connection.setRequestMethod("GET");
    int responseCode = connection.getResponseCode();

    assertEquals(200, responseCode, "Radicale should return HTTP 200");
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "dev-user/d8d512ec-7535-840c-7141-00bc758d6296/");
    registry.add("dav.cal-url", () -> BASE_URL + "dev-user/c028244a-3c9c-d304-5e1e-2f80761d4e66/");
  }

  @Test
  @DisplayName("RADICALE end-to-end test")
  void completeProcess() throws Exception {
    log.info("Starting RADICALE end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
