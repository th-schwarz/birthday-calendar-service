package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.MonthDay;
import net.fortuna.ical4j.vcard.property.BDay;
import org.junit.jupiter.api.Test;

class TemporalUtilTest {

  /**
   * Test the conversion of a valid month-day string in BDay to MonthDay.
   * The input is a valid BDay object with a text representation of "--0415".
   */
  @Test
  void testToMonthDay_ValidMonthDay() {
    // Arrange
    BDay<?> birthday = new BDay<>("--0415");

    // Act
    MonthDay result = TemporalUtil.toMonthDay(birthday);

    // Assert
    assertEquals(MonthDay.of(4, 15), result);
  }

  /**
   * Test the conversion of an invalid month-day string in BDay.
   * The input is a BDay object with an invalid text representation "0415" (missing '--').
   * An IllegalArgumentException is expected.
   */
  @Test
  void testToMonthDay_InvalidFormat() {
    // Arrange
    BDay<?> birthday = new BDay<>("0415");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> TemporalUtil.toMonthDay(birthday),
        "Expected an exception for invalid month-day format."
    );
    assertTrue(exception.getMessage().contains("Not a month day"));
  }

  /**
   * Test the conversion of a BDay object with an invalid day in the month-day string.
   * The input is a BDay object with the text "--0230" (Day 30 in February is invalid).
   * An IllegalArgumentException is expected.
   */
  @Test
  void testToMonthDay_InvalidDayInMonthDay() {
    // Arrange
    BDay<?> birthday = new BDay<>("--02-30");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> TemporalUtil.toMonthDay(birthday),
        "Expected an exception for invalid day in month-day format."
    );
    assertTrue(exception.getMessage().contains("Not a month day"), "Exception message did not contain the expected text.");
  }

  /**
   * Test the conversion of a BDay object with an invalid month in the month-day string.
   * The input is a BDay object with the text "--1320" (Month 13 is invalid).
   * An IllegalArgumentException is expected.
   */
  @Test
  void testToMonthDay_InvalidMonthInMonthDay() {
    // Arrange
    BDay<?> birthday = new BDay<>("--1320");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> TemporalUtil.toMonthDay(birthday),
        "Expected an exception for invalid month in month-day format."
    );
    assertTrue(exception.getMessage().contains("Not a month day"), "Exception message did not contain the expected text.");
  }

  /**
   * Test the behavior of toMonthDay when the BDay contains a null text.
   * The input is a BDay object with null text.
   * An IllegalArgumentException is expected.
   */
  @Test
  void testToMonthDay_NullTextInBDay() {
    // Arrange
    BDay<?> birthday = new BDay<>((String) null);

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> TemporalUtil.toMonthDay(birthday),
        "Expected an exception when BDay text is null."
    );
  }

  /**
   * Test the behavior of toMonthDay when the BDay is valid but has different valid date-like formats.
   * The input tests are not LocalDate instances but valid month-day textual formats.
   */
  @Test
  void testToMonthDay_ValidEdgeCases() {
    // Test cases
    BDay<?> birthday1 = new BDay<>("--0101"); // January 1
    BDay<?> birthday2 = new BDay<>("--1231"); // December 31

    // Act & Assert
    assertEquals(MonthDay.of(1, 1), TemporalUtil.toMonthDay(birthday1));
    assertEquals(MonthDay.of(12, 31), TemporalUtil.toMonthDay(birthday2));
  }
}
