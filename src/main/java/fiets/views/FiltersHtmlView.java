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
    filters.forEach(f -> sb.append(filter(f)));
    return sb.append("</ul>")
      .append(Pages.editFilterTemplate())
      .append(Pages.footerTemplate("")).toString();
  }

  private String filter(Filter f) {
    return String.format(
        "<li class='list-group-item filter' data-id='%d'>%s and %s<span class='filter-actions'>"
        + "<button class='btn btn-sm btn-light edit-filter'>Edit</button>"
        + "<a href='%s' class='btn btn-sm btn-danger delete-filter'>Delete</a></span></li>",
        f.getId(), f.getUrlMatch().displayText("URL", f.getUrl()), f.getTitleMatch().displayText("title", f.getTitle()), deleteFilterLink(f));
  }

  private String deleteFilterLink(Filter f) {
    return "/delete-filter?id=" + f.getId();
  }
}
