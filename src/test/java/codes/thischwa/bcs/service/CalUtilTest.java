package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import codes.thischwa.bcs.Contact;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;
import net.fortuna.ical4j.model.component.VEvent;
import org.junit.jupiter.api.Test;

class CalUtilTest {

  @Test
  void testExtractContactsUuidFromEvent_WithoutVcfExtension() {
    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:12345-abcde
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    VEvent event = parseEvent(icsContent);
    String uuid = CalUtil.extractContactsUuidFromEvent(event);
    assertEquals("12345-abcde", uuid);
  }

  @Test
  void testExtractContactsUuidFromEvent_MissingUid() {
    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    VEvent event = parseEvent(icsContent);
    assertThrows(IllegalArgumentException.class, () ->
        CalUtil.extractContactsUuidFromEvent(event));
  }

  @Test
  void testIsBirthdayEquals_WithMonthDay() {
    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    VEvent event = parseEvent(icsContent);
    Contact contact = new Contact("uuid", "Test", "User", MonthDay.of(4, 15), "test@example.com");
    assertTrue(CalUtil.isBirthdayEquals(event, contact));
  }

  @Test
  void testIsBirthdayEquals_WithLocalDate() {
    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid
        DTSTART;VALUE=DATE:19900415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    VEvent event = parseEvent(icsContent);
    Contact contact = new Contact("uuid", "Test", "User", LocalDate.of(1990, 4, 15), "test@example.com");
    assertTrue(CalUtil.isBirthdayEquals(event, contact));
  }

  @Test
  void testIsBirthdayEquals_NotMatching() {
    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    VEvent event = parseEvent(icsContent);
    Contact contact = new Contact("uuid", "Test", "User", MonthDay.of(5, 15), "test@example.com");
    assertFalse(CalUtil.isBirthdayEquals(event, contact));
  }

  @Test
  void testCollectBirthdayEvents() throws Exception {
    Sardine sardine = mock(Sardine.class);
    String calUrl = "https://example.com/calendars/user/birthday/";

    DavResource resource1 = mock(DavResource.class);
    when(resource1.isDirectory()).thenReturn(false);
    when(resource1.getContentType()).thenReturn("text/calendar");
    when(resource1.getHref()).thenReturn(new URL("https://example.com/calendars/user/birthday/event1.ics").toURI());

    DavResource resource2 = mock(DavResource.class);
    when(resource2.isDirectory()).thenReturn(true);

    when(sardine.list(calUrl)).thenReturn(List.of(resource1, resource2));

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    when(sardine.get(anyString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    Map<VEvent, URL> events = CalUtil.collectBirthdayEvents(sardine, calUrl);

    assertEquals(1, events.size());
    assertNotNull(events.keySet().iterator().next());
  }

  @Test
  void testCollectBirthdayEvents_FiltersNonCalendarContent() throws Exception {
    Sardine sardine = mock(Sardine.class);
    String calUrl = "https://example.com/calendars/user/birthday/";

    DavResource resource1 = mock(DavResource.class);
    when(resource1.isDirectory()).thenReturn(false);
    when(resource1.getContentType()).thenReturn("text/plain");

    when(sardine.list(calUrl)).thenReturn(List.of(resource1));

    Map<VEvent, URL> events = CalUtil.collectBirthdayEvents(sardine, calUrl);

    assertEquals(0, events.size());
  }

  @Test
  void testConvert_ValidEvent() throws Exception {
    Sardine sardine = mock(Sardine.class);
    URL eventUrl = new URL("https://example.com/event.ics");

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday
        END:VEVENT
        END:VCALENDAR
        """;
    when(sardine.get(eventUrl.toString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    VEvent event = CalUtil.convert(sardine, eventUrl);

    assertNotNull(event);
    assertEquals("test-uid", CalUtil.extractContactsUuidFromEvent(event));
  }

  @Test
  void testConvert_NullInputStream() throws Exception {
    Sardine sardine = mock(Sardine.class);
    URL eventUrl = new URL("https://example.com/event.ics");

    when(sardine.get(eventUrl.toString())).thenReturn(null);

    VEvent event = CalUtil.convert(sardine, eventUrl);

    assertNull(event);
  }

  @Test
  void testConvert_MultipleComponents() throws Exception {
    Sardine sardine = mock(Sardine.class);
    URL eventUrl = new URL("https://example.com/event.ics");

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Test//Test//EN
        BEGIN:VEVENT
        UID:test-uid-1
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday 1
        END:VEVENT
        BEGIN:VEVENT
        UID:test-uid-2
        DTSTART;VALUE=DATE:20240416
        SUMMARY:Birthday 2
        END:VEVENT
        END:VCALENDAR
        """;
    when(sardine.get(eventUrl.toString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    assertThrows(IllegalArgumentException.class, () ->
        CalUtil.convert(sardine, eventUrl));
  }

  @Test
  void testConvert_InvalidIcsContent() throws Exception {
    Sardine sardine = mock(Sardine.class);
    URL eventUrl = new URL("https://example.com/event.ics");

    String icsContent = "INVALID CONTENT";
    when(sardine.get(eventUrl.toString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    assertThrows(IllegalArgumentException.class, () ->
        CalUtil.convert(sardine, eventUrl));
  }

  @Test
  void testConvert_IOException() throws Exception {
    Sardine sardine = mock(Sardine.class);
    URL eventUrl = new URL("https://example.com/event.ics");

    when(sardine.get(eventUrl.toString())).thenThrow(new IOException("Network error"));

    assertThrows(IllegalArgumentException.class, () ->
        CalUtil.convert(sardine, eventUrl));
  }

  private VEvent parseEvent(String icsContent) {
    try {
      net.fortuna.ical4j.data.CalendarBuilder builder = new net.fortuna.ical4j.data.CalendarBuilder();
      net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(icsContent.getBytes()));
      return (VEvent) calendar.getComponents().get(0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
