package codes.thischwa.bcs.service;

import codes.thischwa.bcs.conf.DavConf;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * SardineInitializer is responsible for initializing and managing a Sardine client
 * specific to the DAV integration. It handles authentication using credentials provided
 * in the DavConf configuration and provides functionalities for verifying access to
 * the base URL of the DAV server.
 *
 * <p>The class is designed as a Spring component with the prototype scope, creating a new instance
 * for every usage.
 *
 * <p>Features include:
 * <ul>
 * <li>Initialization of a custom Sardine client with a limited redirect strategy.
 * <li>Verification of access to the base URL with retry logic.
 * </ul>
 */
@Component
@Scope("prototype")
@Slf4j
public class SardineInitializer {

  @Getter
  private final Sardine sardine;

  private final DavConf davConf;

  /**
   * Constructs a new SardineInitializer with the given DAV configuration.
   *
   * <p>This constructor initializes the Sardine client using the provided credentials
   * from the DavConf instance for DAV integration.
   *
   * @param davConf The DAV configuration object containing user credentials and other settings.
   */
  public SardineInitializer(DavConf davConf) {
    this.davConf = davConf;
    this.sardine = CustomFactory.begin(davConf.user(), davConf.password());
  }

  /**
   * Checks whether the base URL configured in the DAV configuration is accessible.
   *
   * <p>The method attempts to verify access to the base URL by making requests using the Sardine client.
   * It uses the retry mechanism defined in the configuration, with a specified number of retries
   * and delays between attempts. If the base URL is accessible, the method returns true. If all
   * retries fail or the thread is interrupted, the method returns false.
   *
   * @return true if the base URL is accessible within the defined retry attempts, false otherwise.
   */
  public boolean canAccessBaseUrl() {
    for (int i = 0; i < davConf.maxRetries(); i++) {
      try {
        if (sardine.exists(davConf.getBaseUrl())) {
          return true;
        }
      } catch (IOException e) {
        log.warn("Error while checking access to {} (trails: {}/{}): {}", davConf.getBaseUrl(),
            i + 1, davConf.maxRetries(), e.getMessage());
      }
      try {
        Thread.sleep(davConf.getRetryDelayInMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  private static class CustomFactory {
    static Sardine begin(String username, String password) {
      HttpClientBuilder builder = HttpClientBuilder.create();

      // Set a custom redirect strategy with limited redirects
      builder.setRedirectStrategy(new LimitedRedirectStrategy());

      return new SardineImpl(builder, username, password);
    }

    private static class LimitedRedirectStrategy extends DefaultRedirectStrategy {
      private static final int MAX_REDIRECTS = 3;
      private final ThreadLocal<Integer> redirectCount = ThreadLocal.withInitial(() -> 0);

      @Override
      public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        int count = redirectCount.get();
        if (count >= MAX_REDIRECTS) {
          redirectCount.set(0);
          throw new CircularRedirectException("Maximum redirects (" + MAX_REDIRECTS + ") exceeded");
        }
        redirectCount.set(count + 1);
        return super.getRedirect(request, response, context);
      }
    }
  }
}
