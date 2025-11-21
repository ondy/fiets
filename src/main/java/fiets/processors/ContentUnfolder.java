package fiets.processors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import jodd.jerry.Jerry;

public class ContentUnfolder {
  private static final Logger log = LogManager.getLogger();

  public String unfoldUrls(String snippet, String urlPrefix) {
    return unfoldUrls(snippet, urlPrefix, 0);
  }

  private String unfoldUrls(String snippet, String urlPrefix, int depth) {
    if (depth >= 10) {
      return snippet;
    }
    int urlPos = snippet.indexOf(urlPrefix);
    if (urlPos < 0) {
      return snippet;
    }
    int urlEnd = snippet.indexOf(' ', urlPos);
    if (urlEnd < 0) {
      urlEnd = snippet.length();
    }
    if (urlPos == 0 && urlEnd == snippet.length()) {
      return snippet;
    }
    String url = snippet.substring(urlPos, urlEnd);
    String text = loadUrlContent(url);
    String prefix = snippet.substring(0, urlPos);
    String suffix = snippet.substring(urlEnd);
    log.debug("Unfolded twitter URL %s.", url);
    return unfoldUrls(
      String.format("%s %s %s", prefix, text, suffix), urlPrefix, depth+1);
  }

  private String loadUrlContent(String url) {
    return loadUrlContent(url, 0);
  }

  private String loadUrlContent(String url, int depth) {
    if (depth > 3) {
      return "<error>Maximum redirect depth reached.</error>";
    }
    try {
      boolean fb = url.contains("facebook.com/");
      if (fb) {
        url = appendParam(url, "_fb_noscript=1");
      }
      HttpResponse rsp = HttpRequest.get(url)
        .timeout(10000)
        .connectionTimeout(10000)
        .trustAllCerts(true)
        .send();
      if (rsp.statusCode() == 301) {
        String redirect = rsp.header("location");
        return loadUrlContent(redirect, depth+1);
      }
      String media = rsp.mediaType();
      if (media != null && media.startsWith("image/")) {
        return "[IMAGE]";
      }
      String html = rsp.bodyText();
      if (fb) {
        return extractFacebookArticle(url, html);
      }
      return extractArbitraryArticle(url, html);
    } catch (RuntimeException e) {
      String msg = String.format("Could not extract URL content for %s: %s.",
        url, e.getMessage());
      log.error(msg, e);
      return msg;
    }
  }

  private static String appendParam(String url, String param) {
    char sep = '?';
    if (url.indexOf('?') >= 0) {
      sep = '&';
    }
    return String.format("%s%s%s", url, sep, param);
  }

  private String extractFacebookArticle(String url, String html) {
    Jerry jerry = Jerry.jerry(html);
    return jerry.find("#contentArea").text();
  }

  private String extractArbitraryArticle(String url, String html) {
    String text;
    try {
      text = ArticleExtractor.INSTANCE.getText(html);
    } catch (BoilerpipeProcessingException e) {
      text = String.format("Could not extract integrated URL %s.", url);
      log.error(text, e);
    }
    return text;
  }

  public static void main(String[] args) {
    System.out.println(new ContentUnfolder().loadUrlContent("https://t.co/kTIPi1r5lz"));
  }
}
