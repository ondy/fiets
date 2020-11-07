package fiets.views;

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
      return "javascript:void%20function(a){var%20b=function(a){a.ajax({url:%HOST%/add-feed%22,dataType:%22jsonp%22,data:{url:window.location.href}}).done(function(a){alert(a.title||a.error)})};if(a%26%26a.fn%26%261.7%3C=parseFloat(a.fn.jquery))return%20void%20load(a);var%20c=document.createElement(%22script%22);c.src=%22https://ajax.googleapis.com/ajax/libs/jquery/1/jquery.js%22,c.onload=c.onreadystatechange=function(){var%20a=this.readyState;a%26%26%22loaded%22!==a%26%26%22complete%22!==a||b(jQuery.noConflict())},document.getElementsByTagName(%22head%22)[0].appendChild(c)}(window.jQuery);"
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
