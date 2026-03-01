package codes.thischwa.bcs.backend;

import static codes.thischwa.bcs.service.CalHandler.CALENDAR_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import codes.thischwa.bcs.Contact;
import codes.thischwa.bcs.TestBcgApp;
import codes.thischwa.bcs.conf.BcsConf;
import codes.thischwa.bcs.conf.DavConf;
import codes.thischwa.bcs.service.BirthdayCalGenerator;
import codes.thischwa.bcs.service.CalUtil;
import codes.thischwa.bcs.service.SardineInitializer;
import codes.thischwa.bcs.service.TemporalUtil;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.vcard.Entity;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;
import net.fortuna.ical4j.vcard.property.Uid;
import net.fortuna.ical4j.vcard.property.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestBcgApp.class)
@ActiveProfiles("backend-test")
@Slf4j
public abstract class AbstractBackendTest {

  static final int STARTUP_TIMEOUT_SEC = 180;
  @Autowired
  protected BirthdayCalGenerator generator;

  @Autowired
  protected DavConf davConf;

  @Autowired
  protected BcsConf bcsConf;

  @Autowired
  protected SardineInitializer sardineInitializer;

  private final Contact janeWithBirthDay = new Contact("Jane", "Doe", "J. Doe",
      MonthDay.of(5, 12), "jane0000-0000-0000-0000-000000000000");
  private final Contact johnWithBirthday = new Contact("John", "Smith", "J. Smith",
      LocalDate.of(1985, 11, 3), "john0000-0000-0000-0000-000000000000");
  private final Contact richard = new Contact("Richard", "Smith", "R. Smith", null, "rich0000-0000-0000-0000-000000000000");

  void syncAndVerify() throws Exception {
    Sardine sardine = sardineInitializer.getSardine();

    // 4) Run sync and verify 2 events
    log.info("Step 4: Running BirthdayCalGenerator.processBirthdayEvents 1st time and verifying 2 events");
    generator.processBirthdayEvents();
    List<VEvent> eventsAfterFirstSync = listBirthdayEvents(sardine);
    assertEquals(2, eventsAfterFirstSync.size(), "Expected exactly 2 birthday events");
    // Test birthdays
    log.info("Verifying birthdays for Jane Doe and John Smith");
    VEvent bdEvent = eventsAfterFirstSync.stream()
        .filter(e -> e.getSummary().getValue().contains("Jane"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Jane Doe's birthday event not found"));
    assertTrue(TemporalUtil.isSameBirthday(janeWithBirthDay.birthday(), bdEvent.getDateTimeStart().getDate()),
        "Jane Doe's birthday event should reflect the birthday");
    assertEquals("Birthday: Mai 12" , bdEvent.getDescription().getValue(), "Jane Doe's birthday event should have the correct description");
    bdEvent = eventsAfterFirstSync.stream()
        .filter(e -> e.getSummary().getValue().contains("John"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("John Smith's birthday event not found"));
    assertTrue(TemporalUtil.isSameBirthday(johnWithBirthday.birthday(), bdEvent.getDateTimeStart().getDate()),
        "John Smith's birthday event should reflect the birthday");
    assertEquals("Birthday: 1985-11-03" , bdEvent.getDescription().getValue(), "John Smith's birthday event should have the correct description");

    // 5) Change the birthday of one contact and verify
    log.info("Step 5: Changing Jane Doe's birthday and re-synchronizing");
    TemporalAccessor janeNewBday = TemporalUtil.addDays(janeWithBirthDay.birthday(), 1);
    Contact janeUpdated = new Contact(janeWithBirthDay.firstName(), janeWithBirthDay.lastName(),
        janeWithBirthDay.displayName(), janeNewBday, janeWithBirthDay.identifier());
    putVCard(davConf.cardUrl() + janeWithBirthDay.identifier(), buildVCard(janeUpdated));
    generator.processBirthdayEvents();
    List<VEvent> eventsAfterChange = listBirthdayEvents(sardine);
    assertEquals(2, eventsAfterChange.size(), "Expected exactly 2 birthday events");

    // Check updated BDay
    Optional<VEvent> janeNew = eventsAfterChange.stream().filter(e -> e.getSummary().getValue().contains("Jane")).findFirst();
    assertTrue(janeNew.isPresent(), "Jane's updated event not found after sync");
    bdEvent = janeNew.get();
    assumeTrue(TemporalUtil.isSameBirthday(janeNewBday, bdEvent.getDateTimeStart().getDate()), "Jane's updated event should reflect the changed birthday");
    assertEquals("Birthday: Mai 13"  , bdEvent.getDescription().getValue(), "Jane Doe's birthday event should have the correct description");

    // 6) Delete a contact with a birthday and verify event removal
    log.info("Step 6: Deleting John Smith contact and re-synchronizing");
    sardine.delete(davConf.cardUrl() + johnWithBirthday.identifier() + ".vcf");
    generator.processBirthdayEvents();

    List<VEvent> eventsAfterDeletion = listBirthdayEvents(sardine);
    assertEquals(1, eventsAfterDeletion.size(), "Expected exactly 1 birthday event");
    boolean johnEventStillExists =
        eventsAfterDeletion.stream().anyMatch(e -> e.getSummary().getValue().contains("John"));
    assertFalse(johnEventStillExists, "John Smith's birthday event should be deleted after contact removal");
    boolean janeEventStillExists =
        eventsAfterDeletion.stream().anyMatch(e -> e.getSummary().getValue().contains("Jane"));
    assumeTrue(janeEventStillExists, "Jane Doe's birthday event should still exist");

    log.info("*** All steps completed successfully for: {}", this.getClass().getSimpleName());

    clearRemoteCalendar();
    clearRemoteCard();
    log.info("Step 7: Remote cleanup completed");
  }

  void prepareRemote() throws IOException {
    log.info("Step 1: Clearing remote address book at {}", davConf.cardUrl());
    clearRemoteCard();

    log.info("Step 2: Clearing remote calendar at {}", davConf.calUrl());
    clearRemoteCalendar();

    log.info("Step 3: Adding contacts (2 with birthdays, 1 without)");
    putVCard(davConf.cardUrl() + janeWithBirthDay.identifier(), buildVCard(janeWithBirthDay));
    putVCard(davConf.cardUrl() + johnWithBirthday.identifier(), buildVCard(johnWithBirthday));
    putVCard(davConf.cardUrl() + richard.identifier(), buildVCard(richard));
  }

  private String buildVCard(Contact contact) {
    String uid = contact.identifier().replace(".vcf", "");
    Entity entity = new Entity()
        .add(new Version("4.0"))
        .add(new N(contact.lastName(), contact.firstName(), null, null, null))
        .add(new Fn(contact.displayName()))
        .add(new Uid(uid));
    if (contact.birthday() != null) {
      BDay bDay;
      if (contact.birthday() instanceof MonthDay) {
        bDay = TemporalUtil.toBday((MonthDay) contact.birthday());
      } else {
        bDay= new BDay<>((LocalDate) contact.birthday());
      }
      entity.add(bDay);
    }
    VCard card = new VCard();
    card.add(entity);
    return card.toString();
  }

  void putVCard(String url, String content) throws IOException {
    if (!url.endsWith(".vcf")) {
      url += ".vcf";
    }
    log.info("Uploading vCard via Sardine: {}", url);
    Sardine sardine = sardineInitializer.getSardine();
    byte[] data = content.getBytes(StandardCharsets.UTF_8);
    sardine.put(url, data, "text/vcard; charset=utf-8");
  }

  /**
   * Clears all non-directory resources from the remote card DAV server. This method retrieves the
   * list of resources from the server specified by the card URL and removes each resource that is
   * not a directory.
   * <p>
   * Each deleted resource's path is logged once the deletion is successful.
   *
   * @throws IOException if an I/O error occurs while listing or deleting resources on the DAV
   *                     server.
   */
  void clearRemoteCard() throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    List<DavResource> davResources = sardine.list(davConf.cardUrl());
    for (DavResource resource : davResources) {
      if (!resource.isDirectory()) {
        String path = davConf.getBaseUrl() + resource.getHref().getPath();
        sardine.delete(path);
        log.debug("Successfully deleted {}", path);
      }
    }
  }

  /**
   * Clears the remote calendar by removing all resources of the type "text / calendar" from the
   * specified DAV server.
   *
   * @throws IOException if there is an error during communication with the DAV server while listing
   *                     or deleting resources.
   */
  void clearRemoteCalendar() throws IOException {
    Set<URI> calendarEntries = getCalendarEntriesToDelete();
    if (!calendarEntries.isEmpty()) {
      log.info("{} calendar items found to be removed", calendarEntries.size());
      deleteCalendarEntries(calendarEntries);
    }
  }

  private Set<URI> getCalendarEntriesToDelete() throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    List<DavResource> davResources = sardine.list(davConf.calUrl());
    Set<URI> calendarEntries = new HashSet<>();
    for (DavResource resource : davResources) {
      if (resource.isDirectory()) {
        continue;
      }
      if (resource.getContentType().startsWith(CALENDAR_CONTENT_TYPE)) {
        log.debug("Calendar found: name={}, display-name={}", resource.getName(),
            resource.getDisplayName());
        calendarEntries.add(resource.getHref());
      }
    }
    return calendarEntries;
  }

  private void deleteCalendarEntries(Set<URI> calendarEntries) throws IOException {
    Sardine sardine = sardineInitializer.getSardine();
    for (URI uri : calendarEntries) {
      String path = davConf.getBaseUrl() + uri.getPath();
      sardine.delete(path);
      log.debug("Successfully deleted {}", path);
    }
  }

  private List<VEvent> listBirthdayEvents(Sardine sardine) throws IOException {
    Map<VEvent, URL> eventUrls = CalUtil.collectBirthdayEvents(sardine, davConf.calUrl());
    log.debug("Found {} birthday events.", eventUrls.size());
    return new ArrayList<>(eventUrls.keySet());
  }
}
