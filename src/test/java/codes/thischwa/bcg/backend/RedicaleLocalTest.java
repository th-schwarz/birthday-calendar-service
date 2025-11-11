package codes.thischwa.bcg.backend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Slf4j
@Disabled("Only for local testing")
class RedicaleLocalTest extends AbstractBackendTest {

  private static final String BASE_URL = "http://localhost:8081/";

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("dav.user", () -> "dev-user");
    registry.add("dav.password", () -> "test123");
    registry.add("dav.card-url", () -> BASE_URL + "dev-user/d8d512ec-7535-840c-7141-00bc758d6296/");
    registry.add("dav.cal-url", () -> BASE_URL + "dev-user/c028244a-3c9c-d304-5e1e-2f80761d4e66/");
  }

  @Test
  void completeProcess() throws Exception {
    log.info("Starting RADICALE end-to-end test using env-configured CardDAV/CalDAV endpoints ...");
    prepareRemote();

    syncAndVerify();
  }
}
