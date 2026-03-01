package codes.thischwa.bcs;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Represents a contact with personal details such as name and birthday.
 *
 * @param firstName   The first name of the contact.
 * @param lastName    The last name of the contact.
 * @param displayName The display name of the contact.
 * @param birthday    The birthday of the contact, or null if not specified. Must be an instance of {@link MonthDay} or {@link LocalDate}.
 * @param identifier  The unique identifier of the contact.
 */
public record Contact(String firstName, String lastName, String displayName,
                      @Nullable TemporalAccessor birthday, @Nullable String identifier) {

  /**
   * Constructs a new Contact instance with the specified first name, last name, display name,
   * and birthday. The identifier is set to null by default.
   *
   * @param firstName   The first name of the contact.
   * @param lastName    The last name of the contact.
   * @param displayName The display name of the contact.
   * @param birthday    The birthday of the contact, or null if not specified. Must be an instance of {@link MonthDay} or {@link LocalDate}.
   */
  public Contact(String firstName, String lastName, String displayName, @Nullable TemporalAccessor birthday) {
    this(firstName, lastName, displayName, birthday, null);
  }

  public String getFullName() {
    return String.format("%s %s", firstName, lastName);
  }
}
