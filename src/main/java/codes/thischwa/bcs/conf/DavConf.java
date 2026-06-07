package codes.thischwa.bcs.conf;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Configuration properties for DAV integration. These properties are mapped from configuration
 * sources with the prefix `dav`.
 *
 * @param user                The username for authentication.
 * @param password            The password for authentication.
 * @param baseUrl             The base URL (schema://host(:port)) of the DAV server.
 * @param calPath             The path for accessing calendar services.
 * @param cardPaths           The list of paths for accessing address book services.
 * @param retryDelayInSeconds The delay in seconds for scheduled tasks or updates.
 * @param maxRetries          The maximum number of trials for a specific operation.
 */
@ConfigurationProperties(prefix = "dav")
public record DavConf(
    String user, String password, String baseUrl, String calPath, List<String> cardPaths,
    Integer retryDelayInSeconds, Integer maxRetries) {

  public URI getBaseUri() {
    return UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
  }

  public URI getCalDavUri() {
    return UriComponentsBuilder.fromUriString(baseUrl).path(calPath).build().toUri();
  }

  public List<URI> getCardDavUris() {
    return cardPaths.stream().map(path -> UriComponentsBuilder.fromUriString(baseUrl).path(path).build().toUri()).toList();
  }

  public long getRetryDelayInMillis() {
    return retryDelayInSeconds * 1000L;
  }
}
