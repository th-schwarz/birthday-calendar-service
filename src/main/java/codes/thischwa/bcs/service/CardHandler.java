package codes.thischwa.bcs.service;

import codes.thischwa.bcs.Contact;
import codes.thischwa.bcs.conf.DavConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.ParserException;
import org.springframework.stereotype.Component;

/**
 * Handles operations related to the DAV address book, including fetching and processing contact
 * information such as people with birthdays.
 */
@Slf4j
@Component
public class CardHandler {

  private final DavConf davConf;
  private final SardineInitializer sardineInitializer;

  /**
   * Constructs a new CardHandler instance to manage operations related to DAV address book
   * services.
   *
   * @param davConf            The configuration object containing the credentials and URLs required
   *                           for DAV integration, such as user, password, and the address book
   *                           URL.
   * @param sardineInitializer The initializer for {@link Sardine}.
   */
  public CardHandler(DavConf davConf, SardineInitializer sardineInitializer) {
    this.sardineInitializer = sardineInitializer;
    this.davConf = davConf;
  }

  List<Contact> readContactsWithBirthday() throws IllegalArgumentException {
    if (!sardineInitializer.canAccessBaseUrl()) {
      log.error("Access to {} timed out after {} trails.", davConf.getBaseUrl(),
          davConf.maxRetries());
      throw new IllegalArgumentException("Access to " + davConf.getBaseUrl() + " timed out.");
    }
    Sardine sardine = sardineInitializer.getSardine();
    List<Contact> contacts = new ArrayList<>();
    try {
      List<DavResource> vcardResources = sardine.list(davConf.cardUrl())
          .stream()
          .filter(item -> !item.isDirectory())
          .toList();
      log.info("dav resources found to process: {}", vcardResources.size());

      for (DavResource davResource : vcardResources) {
        String resourceName = (davResource.getDisplayName() == null || davResource.getDisplayName().isEmpty())
            ? davResource.toString() : davResource.getDisplayName();
        log.info("Processing contact: {}", resourceName);
        URI href = new URI(davConf.getBaseUrl() + davResource.getHref().toString());
        readContactFromDav(sardine, href, contacts, resourceName);
      }
      log.info("Contacts with birthday found: {}", contacts.size());
      return contacts;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void readContactFromDav(Sardine sardine, URI href, List<Contact> contacts, String resourceName)
      throws IOException, ParserException {
    try (InputStream vCardStream = sardine.get(href.toString())) {
      Contact contact = CardUtil.buildContact(vCardStream, href);
      contacts.add(contact);
    } catch (MissingBirthdayException mbe) {
      log.debug(mbe.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("Error while processing contact {}: {}", resourceName, e.getMessage());
    }
  }
}
