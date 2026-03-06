package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.thischwa.bcs.Contact;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.MonthDay;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;
import org.junit.jupiter.api.Test;

class CardUtilTest {

  @Test
  void testConvert_ValidVCardWithMonthDay() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        N:Doe;John;;;
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);
    Contact contact = CardUtil.convert(card, "test-uuid");

    assertNotNull(contact);
    assertEquals("John", contact.firstName());
    assertEquals("Doe", contact.lastName());
    assertEquals("test-uuid", contact.identifier());
    assertInstanceOf(MonthDay.class, contact.birthday());
    assertEquals(MonthDay.of(4, 15), contact.birthday());
  }

  @Test
  void testConvert_ValidVCardWithLocalDate() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Jane Smith
        N:Smith;Jane;;;
        BDAY;VALUE=DATE:19900415
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);
    Contact contact = CardUtil.convert(card, "test-uuid-2");

    assertNotNull(contact);
    assertEquals("Jane", contact.firstName());
    assertEquals("Smith", contact.lastName());
    assertEquals("test-uuid-2", contact.identifier());
    assertInstanceOf(LocalDate.class, contact.birthday());
    assertEquals(LocalDate.of(1990, 4, 15), contact.birthday());
  }

  @Test
  void testConvert_ValidVCardWithoutDisplayName() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        N:Doe;John;;;
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);
    Contact contact = CardUtil.convert(card, "test-uuid-3");

    assertNotNull(contact);
    assertEquals("John", contact.firstName());
    assertEquals("Doe", contact.lastName());
    assertEquals("", contact.displayName());
  }

  @Test
  void testConvert_MissingBirthday() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        N:Doe;John;;;
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);

    MissingBirthdayException exception = assertThrows(MissingBirthdayException.class, () ->
        CardUtil.convert(card, "test-uuid"));

    assertTrue(exception.getMessage().contains("Missing birthday"));
    assertTrue(exception.getMessage().contains("Doe"));
  }

  @Test
  void testConvert_MissingName() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);

    assertThrows(IllegalArgumentException.class, () ->
        CardUtil.convert(card, "test-uuid"));
  }

  @Test
  void testBuildContact_WithInputStream() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Alice Johnson
        N:Johnson;Alice;;;
        BDAY;VALUE=TEXT:--0820
        END:VCARD
        """;
    InputStream inputStream = new ByteArrayInputStream(vcfContent.getBytes());
    Contact contact = CardUtil.buildContact(inputStream, "alice-uuid");

    assertNotNull(contact);
    assertEquals("Alice", contact.firstName());
    assertEquals("Johnson", contact.lastName());
    assertEquals("alice-uuid", contact.identifier());
    assertEquals(MonthDay.of(8, 20), contact.birthday());
  }

  @Test
  void testBuildContact_WithURI() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Bob Williams
        N:Williams;Bob;;;
        BDAY;VALUE=DATE:19851210
        END:VCARD
        """;
    InputStream inputStream = new ByteArrayInputStream(vcfContent.getBytes());
    URI uri = new URI("https://example.com/contacts/bob-uuid-123.vcf");
    Contact contact = CardUtil.buildContact(inputStream, uri);

    assertNotNull(contact);
    assertEquals("Bob", contact.firstName());
    assertEquals("Williams", contact.lastName());
    assertEquals("bob-uuid-123", contact.identifier());
  }

  @Test
  void testBuildContact_InvalidVCardContent() {
    String vcfContent = "INVALID VCARD CONTENT";
    InputStream inputStream = new ByteArrayInputStream(vcfContent.getBytes());

    assertThrows(ParserException.class, () ->
        CardUtil.buildContact(inputStream, "test-uuid"));
  }

  @Test
  void testBuildContact_EmptyInputStream() {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);

    assertThrows(Exception.class, () ->
        CardUtil.buildContact(inputStream, "test-uuid"));
  }

  @Test
  void testBuildContact_WithURI_MissingBirthday() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Charlie Brown
        N:Brown;Charlie;;;
        END:VCARD
        """;
    InputStream inputStream = new ByteArrayInputStream(vcfContent.getBytes());
    URI uri = new URI("https://example.com/contacts/charlie-uuid.vcf");

    assertThrows(MissingBirthdayException.class, () ->
        CardUtil.buildContact(inputStream, uri));
  }

  @Test
  void testConvert_WithLeapYearBirthday() throws Exception {
    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Leap Year Baby
        N:Baby;Leap;;;
        BDAY;VALUE=DATE:20000229
        END:VCARD
        """;
    VCard card = parseVCard(vcfContent);
    Contact contact = CardUtil.convert(card, "leap-uuid");

    assertNotNull(contact);
    assertEquals("Leap", contact.firstName());
    assertEquals("Baby", contact.lastName());
    assertEquals(LocalDate.of(2000, 2, 29), contact.birthday());
  }

  private VCard parseVCard(String vcfContent) throws IOException, ParserException {
    VCardBuilder builder = new VCardBuilder(new ByteArrayInputStream(vcfContent.getBytes()));
    return builder.build();
  }
}
