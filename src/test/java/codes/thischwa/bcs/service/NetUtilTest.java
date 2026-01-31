package codes.thischwa.bcs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NetUtil#getBaseUrl(String)}.
 * <p>
 * This test class verifies that the `getBaseUrl` method correctly extracts
 * the base URL from various full URLs, including handling ports, invalid inputs,
 * and default behavior for the protocol and host.
 */
class NetUtilTest {

  @Test
  void testGetBaseUrl_withStandardUrl() {
    String inputUrl = "https://example.org/some/path";
    String expectedBaseUrl = "https://example.org";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl, "Base URL extraction failed for a standard URL.");
  }

  @Test
  void testGetBaseUrl_withUrlContainingPort() {
    String inputUrl = "http://example.org:8080/test";
    String expectedBaseUrl = "http://example.org:8080";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl,
      "Base URL extraction failed for a URL with a port.");
  }

  @Test
  void testGetBaseUrl_withUrlDefaultPortForHttp() {
    String inputUrl = "http://example.org:80/test";
    String expectedBaseUrl = "http://example.org";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl,
      "Base URL should exclude default port 80 for HTTP.");
  }

  @Test
  void testGetBaseUrl_withUrlDefaultPortForHttps() {
    String inputUrl = "https://example.org:443/test";
    String expectedBaseUrl = "https://example.org";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl,
      "Base URL should exclude default port 443 for HTTPS.");
  }

  @Test
  void testGetBaseUrl_withEmptyUrl() {
    String inputUrl = "";
    assertThrows(IllegalArgumentException.class, () -> NetUtil.getBaseUrl(inputUrl),
      "Exception expected for empty URL.");
  }

  @Test
  void testGetBaseUrl_withInvalidUrl() {
    String inputUrl = "invalid-url";
    assertThrows(IllegalArgumentException.class, () -> NetUtil.getBaseUrl(inputUrl),
      "Exception expected for invalid URL.");
  }

  @Test
  void testGetBaseUrl_withUrlContainingQueryAndFragment() {
    String inputUrl = "https://example.org/path?query=param#fragment";
    String expectedBaseUrl = "https://example.org";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl,
      "Base URL should exclude path, query, and fragment parts.");
  }

  @Test
  void testGetBaseUrl_withUrlContainingIP() {
    String inputUrl = "http://192.168.0.1/some/path";
    String expectedBaseUrl = "http://192.168.0.1";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl,
      "Base URL extraction failed for a URL containing an IP.");
  }

  @Test
  void testGetBaseUrl_withUrlContainingPortAndDefaultPortMismatch() {
    String inputUrl = "https://example.org:1234/path";
    String expectedBaseUrl = "https://example.org:1234";
    String actualBaseUrl = NetUtil.getBaseUrl(inputUrl);

    assertEquals(expectedBaseUrl, actualBaseUrl, "Base URL extraction failed for mismatched port.");
  }
}