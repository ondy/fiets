package fiets.views;

import fiets.model.Filter;

import java.util.List;

public class FiltersHtmlView implements View<String> {
  private final String hostname;
  private final List<Filter> filters;
  private final int unreadCount;
  private final int bookmarkCount;

  public FiltersHtmlView(String theHostname, List<Filter> theFilters,
                       int theUnreadCount, int theBookmarkCount) {
    hostname = theHostname;
    filters = theFilters;
    unreadCount = theUnreadCount;
    bookmarkCount = theBookmarkCount;
  }

  @Override public String getMimeType() {
    return "text/html";
  }

  @Override public String getContent() {
    StringBuilder sb = new StringBuilder()
        .append(Pages.headerTemplate(Pages.Name.filters, filters.size() + " filters",
            unreadCount, bookmarkCount))
        .append("<ul class='list-group'>");
    for (Filter f : filters) {
      sb.append("<li class='list-group-item filter");
      /*
      if (!"OK".equals(f.getFeed().getLastStatus())) {
        sb.append("  list-group-item-warning");
      }*/
      sb.append("'>")
          .append(filter(f))
          .append("</li>");
    }
    return sb.append("</ul>").append(
        Pages.footerTemplate("")).toString();
  }

  private String filter(Filter f) {
    return String.format(
        "%s %s %s %s",
        f.getTitle(), f.getTitleMatch(), f.getUrl(), f.getUrlMatch());
  }

}
