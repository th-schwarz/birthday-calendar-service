package codes.thischwa.bcs.service;

/**
 * This exception is thrown when a birthday is missing for a given contact.
 * It extends {@code IllegalArgumentException} to indicate an illegal or
 * inappropriate argument during the processing or validation of a contact's birthday data.
 */
public class MissingBirthdayException extends Exception {

  /**
   * Constructs a new {@code MissingBirthdayException} with a detail message that includes the name
   * of the contact for whom the birthday is missing.
   *
   * @param contactsName the name of the contact whose birthday is missing
   */
  public MissingBirthdayException(String contactsName) {
    super("Missing birthday for: " + contactsName);
  }
}
