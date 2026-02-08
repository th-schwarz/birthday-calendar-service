package codes.thischwa.bcs.conf;

import codes.thischwa.bcs.AbstractTest;
import codes.thischwa.bcs.Contact;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventConfTest extends AbstractTest {

  @Autowired
  private EventConf eventConf;

  @Test
  void testEventConfPropertiesLoadedFromApplicationTestYml() {
    Contact p = new Contact("Firstname", "Lastname", "FirstLast", LocalDate.of(1980, 12, 1));
    assertEquals("\uD83C\uDF82 Firstname Lastname", eventConf.generateSummary(p));
    assertEquals("Birthday: 1980-12-01", eventConf.generateDescription(p));
    assertEquals(Duration.ofDays(-1), eventConf.getAlarmDuration());
  }

  @Test
  void testInvalidDuration() {
    assertThrows(IllegalArgumentException.class, () -> new EventConf("🎂 ~first-name~ ~last-name~", "Birthday: ~birthday~", "yyyy-MM-dd", "30s"));
  }

  @Test
  void testInvalidDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> new EventConf("🎂 ~first-name~ ~last-name~", "Birthday: ~birthday~", "invalid-date-format", "3h")) ;
  }
}
