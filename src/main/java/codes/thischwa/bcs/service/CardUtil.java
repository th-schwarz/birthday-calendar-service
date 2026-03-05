package codes.thischwa.bcs.service;

import codes.thischwa.bcs.Contact;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;
import org.apache.commons.io.IOUtils;

/**
 * Utility class for converting VCard objects into Contact objects.
 *
 * <p>>This class provides a method to transform the properties of a VCard
 * into a structured Contact entity with attributes such as name, birthday,
 * and a unique identifier.
 */
public class CardUtil {

  private CardUtil() {
  }

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
  public static Contact convert(VCard card, String identifier) throws MissingBirthdayException {
    PropertyList propertyList = card.getEntities().get(0).getPropertyList();
    Optional<N> optName = propertyList.getFirst(N.class.getSimpleName());
    N fullName = optName.orElseThrow(() -> new IllegalArgumentException("Missing name"));

    Optional<Fn> optFn = propertyList.getFirst(Fn.class.getSimpleName());
    String displayName = optFn.isPresent() ? optFn.get().getValue() : "";

    String firstName = fullName.getGivenName();
    String lastName = fullName.getFamilyName();

    Optional<BDay<Temporal>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<Temporal> birthday =
        optBday.orElseThrow(() -> new MissingBirthdayException(fullName.toString().trim()));
    TemporalAccessor birthdayDate = TemporalUtil.toTemporal(birthday);
    return new Contact(firstName, lastName, displayName, birthdayDate, identifier);
  }

  /**
   * Builds a Contact object from the provided VCard InputStream and a URI.
   *
   * <p>This method extracts a unique identifier from the URI and uses it to
   * associate with the Contact object created from the VCard data.
   *
   * @param inCard An InputStream containing VCard data. The stream must
   *                    contain valid VCard data.
   * @param href        A URI used to extract a unique identifier for the
   *                    resulting Contact object.
   * @return A Contact instance containing the extracted attributes from the
   *     VCard data, associated with the identifier.
   * @throws IOException     If an I/O error occurs while reading the stream.
   * @throws ParserException If an error occurs during parsing of the VCard data.
   * @throws MissingBirthdayException If the VCard does not contain a valid birthday property.
   */
  public static Contact buildContact(InputStream inCard, URI href) throws IOException, ParserException, MissingBirthdayException {
    String identifier = NetUtil.extractUuId(href.toURL());
    return buildContact(inCard, identifier);
  }

  /**
   * Constructs a Contact object from the provided VCard data stream and a unique identifier.
   *
   * <p>This method reads the VCard data from the input stream, parses it into a VCard object,
   * and then converts it into a Contact object using the provided identifier.
   *
   * @param inCard An InputStream containing VCard data.
   *                    The stream must contain a valid VCard format.
   * @param identifier  A unique string to associate with the resulting Contact object.
   * @return A Contact instance containing the information extracted from the VCard data.
   * @throws IOException     If an I/O error occurs while reading the stream.
   * @throws ParserException If an error occurs during parsing of the VCard data.
   * @throws MissingBirthdayException If the VCard does not contain a valid birthday property.
   */
  public static Contact buildContact(InputStream inCard, String identifier)
      throws IOException, ParserException, MissingBirthdayException {
    byte[] vcfContent = IOUtils.toByteArray(inCard);
    VCardBuilder cardBuilder =
        new VCardBuilder(new ByteArrayInputStream(vcfContent));
    VCard card = cardBuilder.build();
    return convert(card, identifier);
  }
}
