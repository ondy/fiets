package fiets.views;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import fiets.model.Feed;
import fiets.model.FeedInfo;
import fiets.views.Pages.Name;

public class FeedsHtmlView implements View<String> {

  private final String hostname;
  private List<FeedInfo> feeds;
  private int unreadCount;
  private int bookmarkCount;

  public FeedsHtmlView(String theHostname, List<FeedInfo> feedInfos,
    int theUnreadCount, int theBookmarkCount) {
    hostname = theHostname;
    feeds = feedInfos;
    unreadCount = theUnreadCount;
    bookmarkCount = theBookmarkCount;
  }

  @Override public String getMimeType() {
    return "text/html";
  }

  @Override public String getContent() {
    StringBuilder sb = new StringBuilder()
      .append(Pages.headerTemplate(Name.feeds, feeds.size() + " feeds",
        unreadCount, bookmarkCount))
      .append(addFeedBookmarklet())
      .append("<ul class='list-group'>");
    for (FeedInfo f : feeds) {
      sb.append("<li class='list-group-item feed");
      if (!"OK".equals(f.getFeed().getLastStatus())) {
        sb.append("  list-group-item-warning");
      }
      sb.append("'>")
        .append(feed(f))
      .append("</li>");
    }
    return sb.append("</ul>").append(
      Pages.footerTemplate("")).toString();
  }

  private String addFeedBookmarklet() {
    return String.format(
      "<a href='%s' class='btn btn-info bookmarklet'>add-feed bookmarklet</a>",
      bookmarkletUrl());
  }

  private String bookmarkletUrl() {
      return Pages.getResource("static/bookmarklet.js")
        .replace("%HOST%", hostname);
  }

  private String feed(FeedInfo fi) {
    Feed f = fi.getFeed();
    return String.format(
      "<a href='%s'>%s</a><small> (unread: %d, read: %d, last post: %s, status: %s)</small>"
      + "<span class='feed-actions'><a href='%s' class='btn btn-sm btn-light update-feed'>Update</a>"
      + "<a href='%s' class='btn btn-sm btn-danger delete-feed'>Delete</a></span>",
      f.getLocation(), f.getTitle(), fi.getNumUnread(), fi.getNumRead(),
      PostDisplay.fmtDate(fi.getMostRecentPost()), f.getLastStatus(),
      updateFeedLink(f), deleteFeedLink(f));
  }

  private String updateFeedLink(Feed f) {
    return "/update-feed?id=" + f.getId();
  }

  private String deleteFeedLink(Feed f) {
    return "/delete-feed?id=" + f.getId();
  }
}
