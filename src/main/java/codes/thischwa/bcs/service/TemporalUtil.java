package codes.thischwa.bcs.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fortuna.ical4j.vcard.property.BDay;

/**
 * Utility class for handling various operations related to temporal objects
 * such as birthdays and dates. This class provides methods to work with
 * {@code MonthDay}, {@code LocalDate}, and custom birthday representations.
 * It is designed to verify, convert, and compare temporal objects based on
 * specific patterns and structures.
 */
public final class TemporalUtil {

  private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("^--(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$");

  private TemporalUtil() {
  }

  /**
   * Determines if a given birthday is represented as a month-day pattern.
   *
   * @param birthday the birthday object to check, which may contain a textual representation
   *                 of a month-day pattern or a date.
   * @return true if the birthday is in a valid month-day pattern format and not a LocalDate,
   *     otherwise false.
   */
  public static boolean isMonthDay(BDay<?> birthday) {
    return !(birthday.getDate() instanceof LocalDate) && MONTH_DAY_PATTERN.matcher(birthday.getText()).matches();
  }

  /**
   * Converts a birthday object into a {@code MonthDay} instance.
   * The birthday must be in a valid month-day pattern, defined as "--MMDD",
   * where MM represents the month in a 2-digit format (01-12) and DD
   * represents the day in a 2-digit format (01-31).
   *
   * @param birthDay the birthday object containing the textual representation
   *                 of a month-day pattern.
   * @return a {@code MonthDay} instance corresponding to the parsed month and day.
   * @throws IllegalArgumentException if the input birthday text does not match
   *                                  the expected month-day pattern.
   */
  public static MonthDay toMonthDay(BDay<?> birthDay) {
    if (birthDay.getText() == null) {
      throw new IllegalArgumentException("Birthday text is null.");
    }
    Matcher matcher = MONTH_DAY_PATTERN.matcher(birthDay.getText());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Not a month day: " + birthDay.getText());
    }
    String month = matcher.group(1);
    String day = matcher.group(2);
    return MonthDay.of(Integer.parseInt(month), Integer.parseInt(day));
  }

  /**
   * Converts a {@link TemporalAccessor} object into a {@link LocalDate} instance.
   * This method supports {@link LocalDate} and {@link MonthDay} inputs, converting
   * them into a {@link LocalDate} that represents an event date.
   *
   * @param temporal the temporal object to be converted, which must be either
   *                 a {@link LocalDate} or a {@link MonthDay}.
   * @return a {@link LocalDate} representing the event date. If the input is a
   *     {@link LocalDate}, it is returned as-is. If the input is a {@link MonthDay},
   *     it is converted to a {@link LocalDate} using the current year.
   * @throws IllegalArgumentException if the input is neither a {@link LocalDate}
   *                                  nor a {@link MonthDay}.
   */
  public static LocalDate toEventDate(TemporalAccessor temporal) {
    if (temporal instanceof LocalDate localDate) {
      return localDate;
    }
    if (temporal instanceof MonthDay monthDay) {
      return monthDay.atYear(LocalDate.now().getYear());
    }
    throw new IllegalArgumentException("Unsupported temporal type: " + temporal.getClass().getSimpleName());
  }

  /**
   * Converts a {@code MonthDay} instance into a {@code BDay} object.
   * The {@code MonthDay} is represented as a string in the format "--MMDD",
   * where MM is the two-digit month and DD is the two-digit day.
   *
   * @param monthDay the {@code MonthDay} instance to be converted.
   * @return a {@code BDay} object representing the corresponding birthday.
   */
  public static BDay<?> toBday(MonthDay monthDay) {
    String monthDayStr = String.format("--%02d%02d", monthDay.getMonthValue(), monthDay.getDayOfMonth());
    return new BDay<>(monthDayStr);
  }

  /**
   * Converts a {@code BDay} object into a {@code TemporalAccessor} instance.
   * If the provided {@code BDay} represents a month-day pattern, the method returns a
   * {@code MonthDay} instance; otherwise, it returns the {@link TemporalAccessor}
   * representing the date directly from the {@code BDay} object.
   *
   * @param birthDay the {@code BDay} object to be converted, which may represent either
   *                 a specific date or a month-day pattern.
   * @return a {@code TemporalAccessor} instance, which is either a {@code MonthDay} if
   *     the input represents a month-day pattern, or the date associated with the
   *     {@code BDay} object.
   */
  public static TemporalAccessor toTemporal(BDay<?> birthDay) {
    if (isMonthDay(birthDay)) {
      return toMonthDay(birthDay);
    }
    return birthDay.getDate();
  }

  /**
   * Determines whether the given contact's birthday matches the event date.
   *
   * <p>The method supports comparing birthdays represented as {@link MonthDay}
   * or {@link LocalDate} instances.
   *
   * @param contactBirthday a {@link TemporalAccessor} representing the contact's birthday.
   *                        This can be a {@link MonthDay} or a {@link LocalDate}.
   * @param eventDate       a {@link Temporal} representing the event date to compare against.
   *                        This can be converted into a {@link LocalDate} or {@link MonthDay}.
   * @return true if the contact's birthday matches the event date, false otherwise.
   */
  public static boolean isSameBirthday(TemporalAccessor contactBirthday, Temporal eventDate) {
    if (contactBirthday instanceof MonthDay monthDay) {
      MonthDay eventMonthDay = MonthDay.from(eventDate);
      return monthDay.equals(eventMonthDay);
    }
    if (contactBirthday instanceof LocalDate localDate) {
      LocalDate eventLocalDate = LocalDate.from(eventDate);
      return localDate.equals(eventLocalDate);
    }
    return LocalDate.from(contactBirthday).equals(LocalDate.from(eventDate));
  }

  /**
   * Adds a specified number of days to a given {@link TemporalAccessor} object.
   * This method supports {@link MonthDay} and {@link LocalDate} types.
   *
   * @param temporalAccessor the temporal object to which days will be added.
   *                         Must be an instance of {@link MonthDay} or {@link LocalDate}.
   * @param days the number of days to add to the temporal object.
   * @return a new {@link TemporalAccessor} object with the specified number of days added.
   *         If the input is a {@link MonthDay}, the resulting instance will be adjusted
   *         to the current year before adding the days.
   * @throws IllegalArgumentException if the input temporalAccessor is not a
   *                                  {@link MonthDay} or {@link LocalDate}.
   */
  public static TemporalAccessor addDays(TemporalAccessor temporalAccessor, int days) {
    if (temporalAccessor instanceof MonthDay monthDay) {
      return MonthDay.from(monthDay.atYear(LocalDateTime.now().getYear()).plusDays(days));
    } else if (temporalAccessor instanceof LocalDate localDate) {
      return localDate.plusDays(days);
    }
    throw new IllegalArgumentException("Unsupported type: " + temporalAccessor.getClass().getName());
  }

}
