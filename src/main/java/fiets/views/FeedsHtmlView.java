package fiets.views;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import fiets.model.Feed;
import fiets.model.FeedInfo;
import fiets.views.Pages.Name;

public class FeedsHtmlView implements View<String> {

  private List<FeedInfo> feeds;
  private int unreadCount;
  private int bookmarkCount;

  public FeedsHtmlView(List<FeedInfo> feedInfos,
    int theUnreadCount, int theBookmarkCount) {
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

  static String bookmarkletUrl() {
    try {
      String inner = "javascript:(function(){function callback(){(function($){var jQuery=$;$.ajax({url: \"%HOST%/add-feed\",dataType: \"jsonp\",data: {url: window.location.href}}).done(function (data) {alert(data.title || data.error);})})(jQuery.noConflict(true))}var s=document.createElement(\"script\");s.src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\";if(s.addEventListener){s.addEventListener(\"load\",callback,false)}else if(s.readyState){s.onreadystatechange=callback}document.body.appendChild(s);})()";
      inner = URLEncoder.encode(inner, StandardCharsets.ISO_8859_1.name());
      inner = inner.replace("+", "%20").replace("%28", "(").replace("%29", ")");
      return "javascript:(" + inner + ")()";
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
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
