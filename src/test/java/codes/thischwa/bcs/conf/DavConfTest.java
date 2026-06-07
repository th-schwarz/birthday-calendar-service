package codes.thischwa.bcs.conf;

import codes.thischwa.bcs.AbstractTest;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DavConfTest extends AbstractTest {

  @Autowired
  private DavConf davConf;

  @Test
  void testDavConfPropertiesLoadedFromApplicationTestYml() {
    assertEquals("dev", davConf.user());
    assertEquals("strong", davConf.password());
    assertEquals("https://dav.my-domain.org", davConf.baseUrl());
    assertEquals(
        "/SOGo/dav/dav-user/Calendar/46-12345678-5-87654321/",
        davConf.calPath()
    );
    assertEquals(
        List.of(URI.create("https://dav.my-domain.org/SOGo/dav/dav-user/Contacts/personal/")),
        davConf.getCardDavUris()
    );
    assertEquals(2, davConf.retryDelayInSeconds());
    assertEquals(5, davConf.maxRetries());
  }

  @Test
  void testWithoutStartingSlashInUrlPath() {
    DavConf dc = new DavConf("user", "password", "https://dav.my-domain.org", "calpath", List.of("cardpath"), 10, 3);
    assertEquals("https://dav.my-domain.org/calpath", dc.getCalDavUri().toString());
    assertEquals("https://dav.my-domain.org/cardpath", dc.getCardDavUris().get(0).toString());
  }
}