package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import codes.thischwa.bcs.Contact;
import codes.thischwa.bcs.conf.DavConf;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardHandlerTest {

  private DavConf davConf;
  private SardineInitializer sardineInitializer;
  private Sardine sardine;
  private CardHandler cardHandler;

  @BeforeEach
  void setUp() {
    davConf = mock(DavConf.class);
    sardineInitializer = mock(SardineInitializer.class);
    sardine = mock(Sardine.class);

    when(davConf.getBaseUrl()).thenReturn("https://example.com");
    when(davConf.cardUrl()).thenReturn("https://example.com/contacts/");
    when(sardineInitializer.getSardine()).thenReturn(sardine);

    cardHandler = new CardHandler(davConf, sardineInitializer);
  }

  @Test
  void testReadContactsWithBirthday_Success() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource1 = mock(DavResource.class);
    when(resource1.isDirectory()).thenReturn(false);
    when(resource1.getDisplayName()).thenReturn("John Doe");
    when(resource1.getHref()).thenReturn(new URI("/contacts/john.vcf"));

    DavResource resource2 = mock(DavResource.class);
    when(resource2.isDirectory()).thenReturn(false);
    when(resource2.getDisplayName()).thenReturn("Jane Smith");
    when(resource2.getHref()).thenReturn(new URI("/contacts/jane.vcf"));

    when(sardine.list("https://example.com/contacts/")).thenReturn(List.of(resource1, resource2));

    String vcfContent1 = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        N:Doe;John;;;
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;

    String vcfContent2 = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Jane Smith
        N:Smith;Jane;;;
        BDAY;VALUE=DATE:19900520
        END:VCARD
        """;

    when(sardine.get("https://example.com/contacts/john.vcf"))
        .thenReturn(new ByteArrayInputStream(vcfContent1.getBytes()));
    when(sardine.get("https://example.com/contacts/jane.vcf"))
        .thenReturn(new ByteArrayInputStream(vcfContent2.getBytes()));

    List<Contact> contacts = cardHandler.readContactsWithBirthday();

    assertEquals(2, contacts.size());
    assertEquals("John", contacts.get(0).firstName());
    assertEquals("Jane", contacts.get(1).firstName());
  }

  @Test
  void testReadContactsWithBirthday_FiltersMissingBirthday() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource1 = mock(DavResource.class);
    when(resource1.isDirectory()).thenReturn(false);
    when(resource1.getDisplayName()).thenReturn("John Doe");
    when(resource1.getHref()).thenReturn(new URI("/contacts/john.vcf"));

    DavResource resource2 = mock(DavResource.class);
    when(resource2.isDirectory()).thenReturn(false);
    when(resource2.getDisplayName()).thenReturn("No Birthday");
    when(resource2.getHref()).thenReturn(new URI("/contacts/nobd.vcf"));

    when(sardine.list("https://example.com/contacts/")).thenReturn(List.of(resource1, resource2));

    String vcfContent1 = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        N:Doe;John;;;
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;

    String vcfContent2 = """
        BEGIN:VCARD
        VERSION:4.0
        FN:No Birthday
        N:Birthday;No;;;
        END:VCARD
        """;

    when(sardine.get("https://example.com/contacts/john.vcf"))
        .thenReturn(new ByteArrayInputStream(vcfContent1.getBytes()));
    when(sardine.get("https://example.com/contacts/nobd.vcf"))
        .thenReturn(new ByteArrayInputStream(vcfContent2.getBytes()));

    List<Contact> contacts = cardHandler.readContactsWithBirthday();

    assertEquals(1, contacts.size());
    assertEquals("John", contacts.get(0).firstName());
  }

  @Test
  void testReadContactsWithBirthday_FiltersDirectories() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource1 = mock(DavResource.class);
    when(resource1.isDirectory()).thenReturn(true);

    DavResource resource2 = mock(DavResource.class);
    when(resource2.isDirectory()).thenReturn(false);
    when(resource2.getDisplayName()).thenReturn("John Doe");
    when(resource2.getHref()).thenReturn(new URI("/contacts/john.vcf"));

    when(sardine.list("https://example.com/contacts/")).thenReturn(List.of(resource1, resource2));

    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:John Doe
        N:Doe;John;;;
        BDAY;VALUE=TEXT:--0415
        END:VCARD
        """;

    when(sardine.get("https://example.com/contacts/john.vcf"))
        .thenReturn(new ByteArrayInputStream(vcfContent.getBytes()));

    List<Contact> contacts = cardHandler.readContactsWithBirthday();

    assertEquals(1, contacts.size());
  }

  @Test
  void testReadContactsWithBirthday_CannotAccessBaseUrl() {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(false);
    when(davConf.maxRetries()).thenReturn(3);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
        cardHandler.readContactsWithBirthday());

    assertTrue(exception.getMessage().contains("timed out"));
  }

  @Test
  void testReadContactsWithBirthday_NullDisplayName() throws Exception {
    when(sardineInitializer.canAccessBaseUrl()).thenReturn(true);

    DavResource resource = mock(DavResource.class);
    when(resource.isDirectory()).thenReturn(false);
    when(resource.getDisplayName()).thenReturn(null);
    when(resource.getHref()).thenReturn(new URI("/contacts/test.vcf"));

    when(sardine.list("https://example.com/contacts/")).thenReturn(List.of(resource));

    String vcfContent = """
        BEGIN:VCARD
        VERSION:4.0
        FN:Test User
        N:User;Test;;;
        BDAY;VALUE=TEXT:--0101
        END:VCARD
        """;

    when(sardine.get(anyString())).thenReturn(new ByteArrayInputStream(vcfContent.getBytes()));

    List<Contact> contacts = cardHandler.readContactsWithBirthday();

    assertEquals(1, contacts.size());
  }
}
