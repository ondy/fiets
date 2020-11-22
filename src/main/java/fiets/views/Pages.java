package fiets.views;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Pages {
  public enum Name {
    read, unread, bookmarks, feeds, filters;

    public String activeMarker() {
      return "%" + name().toUpperCase(Locale.ENGLISH) + "_ACTIVE%";
    }
  };

  private static final Logger log = LogManager.getLogger();
  private static final String HEADER_TEMPLATE = getResource("html/header.html");
  private static final String FOOTER_TEMPLATE = getResource("html/footer.html");
  private static final String EDIT_FILTER_TEMPLATE = getResource("html/edit-filter.html");

  public static String getResource(String name) {
    try (InputStream stream = Pages.class.getResourceAsStream(name)) {
      try (Scanner s = new Scanner(stream, StandardCharsets.UTF_8.name())) {
        return s.useDelimiter("\\Z").next();
      }
    } catch (IOException e) {
      log.error(e, e);
      return "Could not load resource " + name;
    }
  }

  public static String headerTemplate(Name name,
    String title, int unread, int bookmarks) {
    return HEADER_TEMPLATE
      .replace("%TITLE%", title)
      .replace("%UNREAD_COUNT%",
        unread == -1 ? "" : String.format(
          " (<span class='unread-count'>%d</span>)", unread))
      .replace("%BOOKMARKS_COUNT%",
        bookmarks == -1 ? "" : String.format(
          " (<span class='bookmark-count'>%d</span>)", bookmarks))
      .replace(name.activeMarker(), "active")
      .replaceAll("%.*?_ACTIVE%", "");
  }

  public static String footerTemplate(String footerLinks) {
    return FOOTER_TEMPLATE.replace("%FOOTERLINKS%",
      footerLinks);
  }

  public static String editFilterTemplate() {
    return EDIT_FILTER_TEMPLATE;
  }

  private Pages() {}
}
