package codes.thischwa.bcs.conf;

import codes.thischwa.bcs.AbstractTest;
import codes.thischwa.bcs.Contact;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    assertEquals("yyyy-MM-dd", eventConf.getDateFormat());
    assertEquals(Duration.ofDays(-1), eventConf.getAlarmDuration());
  }

}
