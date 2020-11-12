package fiets.views;

import fiets.model.Feed;
import fiets.model.FeedInfo;
import fiets.views.Pages.Name;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FeedsHtmlView implements View<String> {
  private static final String BOOKMARKLET = "javascript:(function()%7Bfunction%20callback()%7B(function(%24)%7Bvar%20jQuery%3D%24%3B%24.ajax(%7Burl%3A%20%22*HOST*%2Fadd-feed%22%2CdataType%3A%20%22jsonp%22%2Cdata%3A%20%7B%20url%3A%20window.location.href%20%7D%7D).done(function%20(data)%20%7Balert(data.title%20%7C%7C%20data.error)%3B%7D)%7D)(jQuery.noConflict(true))%7Dvar%20s%3Ddocument.createElement(%22script%22)%3Bs.src%3D%22https%3A%2F%2Fajax.googleapis.com%2Fajax%2Flibs%2Fjquery%2F1.11.1%2Fjquery.min.js%22%3Bif(s.addEventListener)%7Bs.addEventListener(%22load%22%2Ccallback%2Cfalse)%7Delse%20if(s.readyState)%7Bs.onreadystatechange%3Dcallback%7Ddocument.body.appendChild(s)%3B%7D)()";
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
      BOOKMARKLET);
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
