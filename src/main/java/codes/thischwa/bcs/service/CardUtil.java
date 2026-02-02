package codes.thischwa.bcs.service;

import codes.thischwa.bcs.Contact;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;

/**
 * Utility class for converting VCard objects into Contact objects.
 *
 * <p>>This class provides a method to transform the properties of a VCard
 * into a structured Contact entity with attributes such as name, birthday,
 * and a unique identifier.
 */
public class CardUtil {

  static final DateTimeFormatter birthdayFormatter =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  /**
   * Converts a given VCard object into a Contact object, extracting relevant properties
   * such as first name, last name, display name, and birthday, and associating them with
   * a unique identifier.
   *
   * @param card       The VCard object to be converted. It must contain required properties such as
   *                   name (N), birthday (BDay), and optionally display name (Fn).
   * @param identifier A unique identifier string to associate with the resulting Contact object.
   * @return A Contact instance containing the extracted and transformed properties from the VCard object.
   * @throws IllegalArgumentException If the VCard lacks required properties such as birthday or name.
   */
  public static Contact convert(VCard card, String identifier) {
    PropertyList propertyList = card.getEntities().get(0).getPropertyList();
    Optional<BDay<LocalDate>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<LocalDate> birthday =
        optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

    Optional<Fn> optFn = propertyList.getFirst(Fn.class.getSimpleName());
    Optional<N> optName = propertyList.getFirst(N.class.getSimpleName());

    String displayName = optFn.isPresent() ? optFn.get().getValue() : "";
    N fullName = optName.orElseThrow(() -> new IllegalArgumentException("Missing name"));

    String firstName = fullName.getGivenName();
    String lastName = fullName.getFamilyName();
    return new Contact(firstName, lastName, displayName,
        LocalDate.parse(birthday.getValue(), CardUtil.birthdayFormatter), identifier);

  }
}
