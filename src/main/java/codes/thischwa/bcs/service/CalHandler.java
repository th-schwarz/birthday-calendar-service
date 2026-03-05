package codes.thischwa.bcs.service;

import codes.thischwa.bcs.Contact;
import codes.thischwa.bcs.conf.BcsConf;
import codes.thischwa.bcs.conf.DavConf;
import codes.thischwa.bcs.conf.EventConf;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.springframework.stereotype.Service;

/**
 * The CalHandler class is responsible for managing calendar-related operations, including clearing
 * remote calendars and uploading calendar events for specific individuals. This class leverages
 * WebDAV for remote calendar management and utilizes configurations for event and calendar
 * properties.
 */
@Service
@Slf4j
public class CalHandler {

  public static final String CALENDAR_CONTENT_TYPE = "text/calendar";
  private final BcsConf conf;
  private final EventConf eventConf;
  private final DavConf davConf;
  private final SardineInitializer sardineInitializer;

  private record ExistingEventData(Map<String, VEvent> existingEvents, Map<String, URL> existingEventUris) {}

  /**
   * Constructor for the CalHandler class.
   *
   * @param conf               The configuration object containing settings for the BCG system.
   * @param eventConf          The configuration object for defining event-related settings.
   * @param davConf            The configuration object containing WebDAV user and password
   *                           details.
   * @param sardineInitializer The initializer for {@link Sardine}.
   */
  CalHandler(BcsConf conf, EventConf eventConf, DavConf davConf,
             SardineInitializer sardineInitializer) {
    this.conf = conf;
    this.eventConf = eventConf;
    this.davConf = davConf;
    this.sardineInitializer = sardineInitializer;
  }

  void syncEventsWithBirthdayChanges(List<Contact> contacts) throws IOException {
    if (!sardineInitializer.canAccessBaseUrl()) {
      log.error("Access to {} timed out after {} trails.", davConf.getBaseUrl(), davConf.maxRetries());
      throw new IllegalArgumentException("Access to " + davConf.getBaseUrl() + " timed out.");
    }
    Sardine sardine = sardineInitializer.getSardine();
    log.info("Syncing birthday events of {} contacts.", contacts.size());

    Map<VEvent, URL> allBirthdayEvents = CalUtil.collectBirthdayEvents(sardine, davConf.calUrl());
    ExistingEventData eventData = buildExistingEventData(allBirthdayEvents);

    deleteOutdatedEvents(sardine, contacts, eventData);
    List<Contact> changedPeople = findChangedContacts(contacts, eventData.existingEvents());

    if (changedPeople.isEmpty()) {
      log.info("No birthday events to update found. Sync stopped.");
      return;
    }

    updateChangedEvents(sardine, changedPeople, eventData.existingEventUris());
  }

  private ExistingEventData buildExistingEventData(Map<VEvent, URL> allBirthdayEvents) {
    Map<String, VEvent> existingEvents = new HashMap<>();
    Map<String, URL> existingEventUris = new HashMap<>();

    for (Map.Entry<VEvent, URL> entry : allBirthdayEvents.entrySet()) {
      String uuid = CalUtil.extractContactsUuidFromEvent(entry.getKey());
      existingEvents.put(uuid, entry.getKey());
      String eventId = NetUtil.extractUuId(entry.getValue());
      existingEventUris.put(eventId, allBirthdayEvents.get(entry.getKey()));
    }

    return new ExistingEventData(existingEvents, existingEventUris);
  }

  private void deleteOutdatedEvents(Sardine sardine, List<Contact> contacts, ExistingEventData eventData) {
    Map<String, Contact> existingContacts = new HashMap<>();
    contacts.forEach(contact -> existingContacts.put(contact.identifier(), contact));

    eventData.existingEvents().keySet().forEach(eventUuid -> {
      if (!existingContacts.containsKey(eventUuid)) {
        deleteEvent(sardine, eventData.existingEventUris().get(eventUuid));
      }
    });
  }

  private void deleteEvent(Sardine sardine, URL eventUri) {
    try {
      sardine.delete(davConf.getBaseUrl() + eventUri.getPath());
      log.debug("Deleted outdated event: {}", eventUri.getPath());
    } catch (IOException e) {
      log.error("Failed to delete outdated event: {}", eventUri.getPath(), e);
    }
  }

  private List<Contact> findChangedContacts(List<Contact> contacts, Map<String, VEvent> existingEvents) {
    List<Contact> changedPeople = new ArrayList<>();

    for (Contact contact : contacts) {
      VEvent existingEvent = existingEvents.get(contact.identifier());
      if (existingEvent == null || !CalUtil.isBirthdayEquals(existingEvent, contact)) {
        changedPeople.add(contact);
        log.debug("Found new or updated event found for: {}", contact.getFullName());
      }
    }

    return changedPeople;
  }

  private void updateChangedEvents(Sardine sardine, List<Contact> changedPeople, Map<String, URL> existingEventUris) throws IOException {
    for (Contact contact : changedPeople) {
      Calendar personCal = buildBirthdayCalendar(contact);
      String uuid = contact.identifier();
      if (uuid == null) {
        throw new IllegalArgumentException("Contact identifier must not be null.");
      }

      if (existingEventUris.containsKey(uuid)) {
        URL eventUri = existingEventUris.get(uuid);
        sardine.delete(davConf.getBaseUrl() + eventUri.getPath());
        log.debug("Deleted outdated event before add: {}", eventUri.getPath());
      }

      uploadSingleEvent(sardine, personCal, contact);
      log.info("Added or updated event for: {}", contact.getFullName());
    }
  }

  private Calendar buildBirthdayCalendar(Contact contact) {
    Version version = new Version();
    version.setValue(Version.VALUE_2_0);
    Calendar calendar = new Calendar();
    calendar.add(new ProdId(conf.getProdId()));
    calendar.add(version);
    calendar.add(new CalScale(CalScale.VALUE_GREGORIAN)); //

    VEvent birthdayEvent = buildBirthdayEvent(contact);
    calendar.add(birthdayEvent);
    return calendar;
  }

  private Summary buildSummary(Contact contact) {
    String summary = eventConf.generateSummary(contact);
    return new Summary(summary);
  }

  /**
   * Builds a VEvent instance for a specified person's birthday. The event is annually repeated.
   *
   * @param contact the Person object containing the birthday and other related information
   * @return the constructed VEvent representing the person's birthday
   */
  private VEvent buildBirthdayEvent(Contact contact) {
    assert contact.birthday() != null;
    Summary summary = buildSummary(contact);
    String description = eventConf.generateDescription(contact);

    // Create the birthday event as an all-day event
    LocalDate birthday = TemporalUtil.toEventDate(contact.birthday());
    VEvent birthdayEvent =
        new VEvent(birthday, Duration.ofDays(1), summary.getValue());

    // add the UID
    birthdayEvent.add(new Uid(contact.identifier()));

    // add the repetition rule
    birthdayEvent.add(new RRule<>(new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build()));

    if (eventConf.getAlarmDuration() != null) {
      // build and add an alarm with negative duration (trigger before event)
      VAlarm alarm = new VAlarm(eventConf.getAlarmDuration());
      alarm.add(new Action(Action.VALUE_DISPLAY));
      alarm.add(new Description(description));
      alarm.add(summary);
      birthdayEvent.add(alarm);
    }

    // add other properties
    birthdayEvent.add(new Categories(conf.calendarCategory()));
    birthdayEvent.add(new Transp(Transp.VALUE_TRANSPARENT));
    birthdayEvent.add(new Description(description));
    birthdayEvent.add(new Status(Status.VALUE_CONFIRMED));
    return birthdayEvent;
  }

  private void uploadSingleEvent(Sardine sardine, Calendar calendar, Contact contact) throws IOException {
    String eventContent = calendar.toString();
    String eventUrl = davConf.calUrl() + contact.identifier() + ".ics";
    // Use byte[] upload to ensure Content-Length is set (some servers reject chunked) and send a minimal Content-Type
    byte[] bytes = eventContent.getBytes(StandardCharsets.UTF_8);
    try {
      sardine.put(eventUrl, bytes, CALENDAR_CONTENT_TYPE);
      log.debug("Uploaded birthday event for '{}': {}\n{}", contact.getFullName(), eventUrl, eventContent);
    } catch (IOException e) {
      log.error("Failed to upload birthday event for '{}': {}\n{}", contact.getFullName(), eventUrl, eventContent, e);
      throw e;
    }
  }
}
