package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import codes.thischwa.bcs.Contact;
import codes.thischwa.bcs.conf.BcsConf;
import codes.thischwa.bcs.conf.DavConf;
import codes.thischwa.bcs.conf.EventConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.MonthDay;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalHandlerTest {

  private BcsConf bcsConf;
  private EventConf eventConf;
  private DavConf davConf;
  private SardineInitializer sardineInitializer;
  private Sardine sardine;
  private CalHandler calHandler;

  @BeforeEach
  void setUp() {
    bcsConf = mock(BcsConf.class);
    eventConf = mock(EventConf.class);
    davConf = mock(DavConf.class);
    sardineInitializer = mock(SardineInitializer.class);
    sardine = mock(Sardine.class);

    when(davConf.getBaseUrl()).thenReturn("https://example.com");
    when(davConf.calUrl()).thenReturn("https://example.com/calendars/birthday/");
    when(sardineInitializer.getSardine()).thenReturn(sardine);
    when(bcsConf.getProdId()).thenReturn("//Test//BCS//EN");
    when(bcsConf.calendarCategory()).thenReturn("BIRTHDAY");
    when(eventConf.generateSummary(any())).thenReturn("Birthday: Test");
    when(eventConf.generateDescription(any())).thenReturn("Birthday event");

    calHandler = new CalHandler(bcsConf, eventConf, davConf, sardineInitializer);
  }

  @Test
  void testSyncEventsWithBirthdayChanges_CannotAccessBaseUrl() {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(false);
    when(davConf.maxRetries()).thenReturn(3);

    List<Contact> contacts = List.of(
        new Contact("John", "Doe", "John Doe", MonthDay.of(4, 15), "uuid-1")
    );

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        calHandler.syncEventsWithBirthdayChanges(contacts));

    assertTrue(exception.getMessage().contains("timed out"));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_NoChanges() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource = mock(DavResource.class);
    when(resource.isDirectory()).thenReturn(false);
    when(resource.getContentType()).thenReturn("text/calendar");
    when(resource.getHref()).thenReturn(new URI("/calendars/birthday/uuid-1.ics"));

    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of(resource));

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID://Test//BCS//EN
        BEGIN:VEVENT
        UID:uuid-1
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday: John Doe
        RRULE:FREQ=YEARLY
        END:VEVENT
        END:VCALENDAR
        """;

    when(sardine.get(anyString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    Contact contact = new Contact("John", "Doe", "John Doe", MonthDay.of(4, 15), "uuid-1");
    List<Contact> contacts = List.of(contact);

    calHandler.syncEventsWithBirthdayChanges(contacts);

    verify(sardine, never()).put(anyString(), any(byte[].class), anyString());
  }

  @Test
  void testSyncEventsWithBirthdayChanges_AddNewEvent() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);
    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of());

    Contact contact = new Contact("Jane", "Smith", "Jane Smith", MonthDay.of(5, 20), "uuid-2");
    List<Contact> contacts = List.of(contact);

    calHandler.syncEventsWithBirthdayChanges(contacts);

    verify(sardine).put(eq("https://example.com/calendars/birthday/uuid-2.ics"),
        any(byte[].class), eq("text/calendar"));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_UpdateExistingEvent() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource = mock(DavResource.class);
    when(resource.isDirectory()).thenReturn(false);
    when(resource.getContentType()).thenReturn("text/calendar");
    when(resource.getHref()).thenReturn(new URI("/calendars/birthday/uuid-1.ics"));

    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of(resource));

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID://Test//BCS//EN
        BEGIN:VEVENT
        UID:uuid-1
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday: John Doe
        RRULE:FREQ=YEARLY
        END:VEVENT
        END:VCALENDAR
        """;

    when(sardine.get(anyString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    Contact contact = new Contact("John", "Doe", "John Doe", MonthDay.of(6, 10), "uuid-1");
    List<Contact> contacts = List.of(contact);

    calHandler.syncEventsWithBirthdayChanges(contacts);

    verify(sardine).delete("https://example.com/calendars/birthday/uuid-1.ics");
    verify(sardine).put(eq("https://example.com/calendars/birthday/uuid-1.ics"),
        any(byte[].class), eq("text/calendar"));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_DeleteOutdatedEvent() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource = mock(DavResource.class);
    when(resource.isDirectory()).thenReturn(false);
    when(resource.getContentType()).thenReturn("text/calendar");
    when(resource.getHref()).thenReturn(new URI("/calendars/birthday/uuid-old.ics"));

    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of(resource));

    String icsContent = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID://Test//BCS//EN
        BEGIN:VEVENT
        UID:uuid-old
        DTSTART;VALUE=DATE:20240415
        SUMMARY:Birthday: Old Contact
        RRULE:FREQ=YEARLY
        END:VEVENT
        END:VCALENDAR
        """;

    when(sardine.get(anyString())).thenReturn(new ByteArrayInputStream(icsContent.getBytes()));

    Contact contact = new Contact("New", "Contact", "New Contact", MonthDay.of(5, 20), "uuid-new");
    List<Contact> contacts = List.of(contact);

    calHandler.syncEventsWithBirthdayChanges(contacts);

    verify(sardine).delete("https://example.com/calendars/birthday/uuid-old.ics");
    verify(sardine).put(eq("https://example.com/calendars/birthday/uuid-new.ics"),
        any(byte[].class), eq("text/calendar"));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_NullIdentifier() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);
    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of());

    Contact contact = new Contact("No", "Id", "No Id", MonthDay.of(5, 20), null);
    List<Contact> contacts = List.of(contact);

    assertThrows(IllegalArgumentException.class, () ->
        calHandler.syncEventsWithBirthdayChanges(contacts));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_MultipleContacts() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);
    when(sardine.list("https://example.com/calendars/birthday/")).thenReturn(List.of());

    Contact contact1 = new Contact("John", "Doe", "John Doe", MonthDay.of(4, 15), "uuid-1");
    Contact contact2 = new Contact("Jane", "Smith", "Jane Smith", MonthDay.of(5, 20), "uuid-2");
    Contact contact3 = new Contact("Bob", "Johnson", "Bob Johnson", MonthDay.of(6, 10), "uuid-3");
    List<Contact> contacts = List.of(contact1, contact2, contact3);

    calHandler.syncEventsWithBirthdayChanges(contacts);

    verify(sardine, times(3)).put(anyString(), any(byte[].class), eq("text/calendar"));
  }

  @Test
  void testSyncEventsWithBirthdayChanges_IOException() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);
    when(sardine.list("https://example.com/calendars/birthday/"))
        .thenThrow(new IOException("Connection failed"));

    Contact contact = new Contact("Test", "User", "Test User", MonthDay.of(3, 15), "uuid-1");
    List<Contact> contacts = List.of(contact);

    assertThrows(IOException.class, () ->
        calHandler.syncEventsWithBirthdayChanges(contacts));
  }
}
