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
        .append("<table class='table'><thead><tr><th>URL Match</th><th>URL</th><th>Title Match</th><th>Title</th></tr></thead><tbody>");
    filters.forEach(f -> sb.append(filter(f)));
    return sb.append("</tbody></table>").append(
        Pages.footerTemplate("")).toString();
  }

  private String filter(Filter f) {
    return String.format(
        "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
        f.getTitleMatch(), f.getTitle(), f.getUrlMatch(), f.getUrl());
  }

}