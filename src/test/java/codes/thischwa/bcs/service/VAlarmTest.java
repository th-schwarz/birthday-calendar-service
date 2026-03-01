package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.junit.jupiter.api.Test;

class VAlarmTest {

  @Test
  void testVAlarmWithNegativeDuration() {
    Version version = new Version();
    version.setValue(Version.VALUE_2_0);
    Calendar calendar = new Calendar();
    calendar.add(new ProdId("test"));
    calendar.add(version);
    calendar.add(new CalScale(CalScale.VALUE_GREGORIAN));

    LocalDate birthday = LocalDate.of(1985, 11, 3);
    VEvent birthdayEvent = new VEvent(birthday, Duration.ofDays(1), "Test Birthday");
    birthdayEvent.add(new Uid("test-uid"));
    birthdayEvent.add(new RRule<>(new Recur.Builder<LocalDate>().frequency(Frequency.YEARLY).build()));

    Duration alarmDuration = Duration.ofDays(-1);
    VAlarm alarm = new VAlarm(alarmDuration);
    alarm.add(new Action(Action.VALUE_DISPLAY));
    alarm.add(new Description("Test Description"));
    alarm.add(new Summary("Test Summary"));
    birthdayEvent.add(alarm);

    calendar.add(birthdayEvent);

    String icalString = calendar.toString();
    System.out.println("Generated iCal:");
    System.out.println(icalString);

    // Check that the TRIGGER is present and formatted correctly
    assertTrue(icalString.contains("TRIGGER:-P1D") || icalString.contains("TRIGGER;VALUE=DURATION:-P1D"),
        "TRIGGER should contain -P1D");
  }
}
