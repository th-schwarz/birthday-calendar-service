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

@Component
@Scope("prototype")
@Slf4j
public class SardineInitializer {

  @Getter
  private final Sardine sardine;

  private final DavConf davConf;

  public SardineInitializer(DavConf davConf) {
    this.davConf = davConf;
    this.sardine = CustomFactory.begin(davConf.user(), davConf.password());
  }

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
