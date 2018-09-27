package fiets.processors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Xml {
  private static final Logger log = LogManager.getLogger();
  private static String[] PUB_DATE_PATTERNS = new String[] {
    "dd MMM yyyy",
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss 'Z'",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss'Z'"
  };

  public static String dropSignature(String content) {
    if (!content.startsWith("<?xml")) {
      return content;
    }
    int sigEnd = content.indexOf("?>");
    if (sigEnd < 0) {
      throw new IllegalArgumentException(
        "Malformed content, no signature end found: " + shorten(content));
    }
    return dropSignature(content.substring(sigEnd+2).trim());
  }

  private static String shorten(String content) {
    if (content.length() > 30) {
      content = content.substring(0, 30);
    }
    return content;
  }

  public static String dropComments(String content) {
    if (!content.startsWith("<!--")) {
      return content;
    }
    int commEnd = content.indexOf("-->");
    if (commEnd < 0) {
      throw new IllegalArgumentException(
        "Malformed content, no comment end found: " + shorten(content));
    }
    return dropComments(content.substring(commEnd+3).trim());
  }
  
  public static Date parseDate(String dateString) {
    for (String pattern : PUB_DATE_PATTERNS) {
      try {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).parse(dateString);
      } catch (ParseException e) {
        log.debug(
          "Could not parse date {} with pattern {}.", dateString, pattern);
      }
    }
    throw new IllegalArgumentException("Unparseable date: " + dateString);
  }

  private Xml() {}
}
