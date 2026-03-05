package net.fortuna.ical4j.vcard;

import codes.thischwa.bcs.service.TemporalUtil;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.Temporal;
import java.util.Optional;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.validate.ValidationResult;
import net.fortuna.ical4j.vcard.property.BDay;
import net.fortuna.ical4j.vcard.property.Fn;
import net.fortuna.ical4j.vcard.property.N;
import net.fortuna.ical4j.vcard.property.Uid;
import net.fortuna.ical4j.vcard.property.Version;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VcardTest {

  @Test
  void testRead30BdFull() throws ParserException, IOException {
    VCardBuilder builder = new VCardBuilder(this.getClass().getResourceAsStream("30-bd-full.vcf"));
    VCard card = builder.build();
    PropertyList propertyList = card.getEntities().get(0).getPropertyList();
    Optional<BDay<?>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<?> birthday =
        optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

    assertEquals("19880829", birthday.getValue());
  }

  @Test
  void testRead30BdWithoutYear() throws ParserException, IOException {
    VCardBuilder builder = new VCardBuilder(this.getClass().getResourceAsStream("30-bd-without-year.vcf"));
    VCard card = builder.build();
    PropertyList propertyList = card.getEntities().get(0).getPropertyList();
    Optional<BDay<?>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<?> birthday =
        optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

    assertEquals("--0415", birthday.getValue());
    assertEquals(MonthDay.of(4, 15), TemporalUtil.toMonthDay(birthday));
  }

  @Test
  void testBuildVcardFullBirthday() throws Exception {
    Entity entity = new Entity()
        .add(new Version("4.0"))
        .add(new Fn("Ada Lovelace"))
        .add(new N("Lovelace", "Ada", null, null, null))
        .add(new BDay<>(LocalDate.of(1815, 12, 10)))
        .add(new Uid(URI.create("urn:uuid:123e4567-e89b-12d3-a456-426614174000")));

    VCard card = new VCard();
    card.add(entity);

    ValidationResult result = card.validate();
    assertFalse(result.hasErrors(), () -> "Validation errors: " + result.getEntries());
    assertEquals(1, card.getEntities().size());
    assertEquals("BEGIN:VCARD\n"
        + "VERSION:4.0\n"
        + "FN:Ada Lovelace\n"
        + "N:Lovelace;Ada;;;\n"
        + "BDAY;VALUE=DATE:18151210\n"
        + "UID:urn:uuid:123e4567-e89b-12d3-a456-426614174000\n"
        + "END:VCARD\n", card.toString().replaceAll("\r", ""));

    VCard cardRead = read(card.toString());
    PropertyList propertyList = cardRead.getEntities().get(0).getPropertyList();
    Optional<BDay<Temporal>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<Temporal> birthday =
        optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

    Temporal bday = birthday.getDate();
    assertEquals(LocalDate.of(1815, 12, 10), bday);
  }

  @Test
  void testBuildVcardMonthDayBirthday() throws Exception {
    Entity entity = new Entity()
        .add(new Version("4.0"))
        .add(new Fn("Ada Lovelace"))
        .add(new N("Lovelace", "Ada", null, null, null))
        .add(new BDay<>("--0130"))
        .add(new Uid(URI.create("urn:uuid:123e4567-e89b-12d3-a456-426614174000")));

    VCard card = new VCard();
    card.add(entity);

    ValidationResult result = card.validate();
    assertFalse(result.hasErrors(), () -> "Validation errors: " + result.getEntries());
    assertEquals(1, card.getEntities().size());
    assertEquals("BEGIN:VCARD\n"
        + "VERSION:4.0\n"
        + "FN:Ada Lovelace\n"
        + "N:Lovelace;Ada;;;\n"
        + "BDAY;VALUE=TEXT:--0130\n"
        + "UID:urn:uuid:123e4567-e89b-12d3-a456-426614174000\n"
        + "END:VCARD\n", card.toString().replaceAll("\r", ""));

    VCard cardRead = read(card.toString());
    PropertyList propertyList = cardRead.getEntities().get(0).getPropertyList();
    Optional<BDay<?>> optBday = propertyList.getFirst(BDay.class.getSimpleName());
    BDay<?> birthday =
        optBday.orElseThrow(() -> new IllegalArgumentException("Missing birthday"));

    assertTrue(TemporalUtil.isMonthDay(birthday));
    assertEquals(MonthDay.of(1, 30), TemporalUtil.toMonthDay(birthday));
  }

  private VCard read(String content) throws Exception {
    VCardBuilder builder = new VCardBuilder(new StringReader(content));
      return builder.build();
  }
}
