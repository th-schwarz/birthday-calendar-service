package codes.thischwa.bcs.service;

import java.net.URL;

/**
 * Utility class for performing network-related operations.
 */
public class NetUtil {

  /**
   * Extracts the base URL from a given full URL.
   * The base URL includes the protocol, host, and optionally the port if it is explicitly defined
   * and not the default for the given protocol.
   *
   * @param url the full URL as a string from which the base URL is to be extracted
   * @return the base URL as a string
   * @throws IllegalArgumentException if the provided URL is invalid
   */
  public static String getBaseUrl(String url) {
    try {
      URL parsedUrl = new URL(url);
      String protocol = parsedUrl.getProtocol();
      String host = parsedUrl.getHost();
      int port = parsedUrl.getPort();

      // Only include port if it's explicitly specified and not the default for the protocol
      if (port != -1 && port != parsedUrl.getDefaultPort()) {
        return protocol + "://" + host + ":" + port;
      } else {
        return protocol + "://" + host;
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid URL: " + url, e);
    }
  }

  /**
   * Extracts the UUID from the provided URL's path. The method takes the last segment of the URL's
   * path, removes the file extension if present, and returns it as the UUID.
   *
   * @param inputUrl the URL object from which the UUID is to be extracted
   * @return the extracted UUID as a string
   */
  public static String extractUuId(URL inputUrl) {
    String path = inputUrl.getPath();
    String fileName = path.substring(path.lastIndexOf('/') + 1);
    if (fileName.contains(".")) {
      // Remove the file extension
      fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    }
    return fileName;
  }
}
