package fiets.views;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import fiets.model.Post;

public class PostDisplay {
  private static final Logger log = LogManager.getLogger();

  private final Post post;

  public PostDisplay(Post thePost) {
    post = thePost;
  }

  public String getTitle() {
    return post.getTitle();
  }

  public String getShortenedTitle() {
    return shorten(getTitle(), 90);
  }
 
  public String getShortenedSnippet() {
    String snippet = post.getSnippet();
    if (snippet == null || snippet.trim().length() == 0) {
      snippet = post.getTitle();
    }
    return shorten(snippet, 330);
  }

  public String getDate() {
    return fmtDate(post.getDate());
  }

  public static String fmtDate(Date date) {
    return date == null ? "--" :
      new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH).format(date);
  }

  public String getFeedTitle() {
    return cleanup(post.getFeed().getTitle());
  }

  public String getLocation() {
    return cleanup(post.getLocation());
  }

  private static String shorten(String text, int maxLength) {
    if (text.length() > maxLength) {
      text = text.substring(0, maxLength-1) + " [&hellip;]";
    }
    return text;
  }

  private static String cleanup(String text) {
    try {
      return Jsoup.parse(text).text()
              .replace("'", "&apos;").replace("\"", "&quot;");
    } catch (RuntimeException e) {
      log.error("Parser error: " + e, e);
      return text;
    }
  }

}
