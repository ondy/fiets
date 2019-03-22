package fiets.sources;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.model.Feed;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public class HttpFeedSource implements FeedSource {
  public static final String UTF8_BOM = "\uFEFF";
  private static final Logger log = LogManager.getLogger();
  
  @Override public String process(Feed feed) {
    HttpResponse rsp = HttpRequest.get(feed.getLocation())
      .trustAllCerts(true)
      .followRedirects(true)
      .send();
    int status = rsp.statusCode();
    if (status != 200) {
      log.error("Unexpected status for {} : {}", feed.getLocation(), status);
    }
    String text = rsp.bodyText();
    if (text.startsWith(UTF8_BOM)) {
      text = text.substring(1);
    }
    return text.trim();
  }

  @Override public boolean canHandle(Feed feed) {
    return isHttpSource(feed);
  }

  public static boolean isHttpSource(Feed feed) {
    try {
      String url = feed.getLocation().toLowerCase(Locale.ROOT);
      return url.startsWith("http://") || url.startsWith("https://");
    } catch (RuntimeException e) {
      log.debug("Unexpected issues: " + e, e);
      return false;
    }
  }

  public static Map<String, List<String>> parseQueryString(
    String url, Charset charset) throws UnsupportedEncodingException {
    Map<String, List<String>> result = new HashMap<>();
    int qmPos = url.indexOf('?');
    if (qmPos < 0) {
      return result;
    }
    String[] params = url.substring(qmPos+1).split("&");
    for (String param : params) {
      int eqPos = param.indexOf('=');
      String name;
      String value = null;
      if (eqPos < 0) {
        name = param;
      } else {
        name = param.substring(0, eqPos);
        value = URLDecoder.decode(param.substring(eqPos+1), charset.name());
      }
      if (!result.containsKey(name)) {
        result.put(name, new ArrayList<>());
      }
      result.get(name).add(value);
    }
    return result;
  }
}
