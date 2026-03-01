package codes.thischwa.bcs.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import codes.thischwa.bcs.AbstractTest;
import codes.thischwa.bcs.Contact;
import java.time.Duration;
import java.time.LocalDate;
import java.time.MonthDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventConfTest extends AbstractTest {

  @Autowired
  private EventConf eventConf;

  @Test
  void testConf() {
   assertEquals(Duration.ofDays(-1), eventConf.getAlarmDuration());
  }

  @Test
  void testContactLocalDate() {
    Contact p = new Contact("Firstname", "Lastname", "FirstLast", LocalDate.of(1980, 12, 1));
    assertEquals("\uD83C\uDF82 Firstname Lastname", eventConf.generateSummary(p));
    assertEquals("Birthday: 1980-12-01", eventConf.generateDescription(p));
    assertEquals(Duration.ofDays(-1), eventConf.getAlarmDuration());
  }
  @Test

  void testContactMonthDay() {
    Contact p = new Contact("Firstname", "Lastname", "FirstLast", MonthDay.of(12, 1));
    assertEquals("\uD83C\uDF82 Firstname Lastname", eventConf.generateSummary(p));
    assertEquals("Birthday: 12-01", eventConf.generateDescription(p));
    assertEquals(Duration.ofDays(-1), eventConf.getAlarmDuration());
  }

  @Test
  void testInvalidDuration() {
    assertThrows(IllegalArgumentException.class,
        () -> new EventConf("🎂 ~first-name~ ~last-name~", "Birthday: ~birthday~", "yyyy-MM-dd", "MM--dd",
            "30s"));
  }

  @Test
  void testInvalidDateFormat() {
    assertThrows(IllegalArgumentException.class,
        () -> new EventConf("🎂 ~first-name~ ~last-name~", "Birthday: ~birthday~", "invalid-date-format", "MM-dd",
            "3h"));
    assertThrows(IllegalArgumentException.class,
        () -> new EventConf("🎂 ~first-name~ ~last-name~", "Birthday: ~birthday~", "MM-dd", "invalid-date-format",
            "3h"));
  }
}
