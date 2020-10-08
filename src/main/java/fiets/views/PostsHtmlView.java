package fiets.views;

import java.util.List;
import java.util.Set;

import fiets.model.Post;
import fiets.views.Pages.Name;

public class PostsHtmlView implements View<String> {

  private final List<Post> posts;
  private final int unreadCount;
  private final Set<Long> bookmarked;
  private Name pageName;

  public PostsHtmlView(
    Name thePageName,
    List<Post> thePosts, Set<Long> theBookmarked, int theUnreadCount) {
    pageName = thePageName;
    posts = thePosts;
    bookmarked = theBookmarked;
    unreadCount = theUnreadCount;
  }

  @Override public String getMimeType() {
    return "text/html";
  }

  @Override public String getContent() {
    StringBuilder sb = new StringBuilder()
      .append(header())
      .append("<ul class='list-group'>");
    for (Post p : posts) {
      sb.append(post(p));
    }
    return sb.append("</ul>").append(
      Pages.footerTemplate(markReadLink())).toString();
  }

  private String post(Post p) {
    PostDisplay display = new PostDisplay(p);
    return String.format(
      "<li class='list-group-item post %s'>"
      + "<small>%s</small> | <small>%s</small>"
      + "<span class='post-actions'>"
      + addFilterLink(p)
      + bookmarkLink(p)
      + removeBookmarkLink(p)
      + "</span>"
      + "<h3 title='%s'><a href='%s' target='_blank'>%s</h4></a>"
      + "<div>%s</div></li>",
      isBookmarked(p) ? "bookmarked" : "",
      display.getDate(), display.getFeedTitle(),
      display.getTitle(), p.getLocation(),
      display.getShortenedTitle(),
      display.getShortenedSnippet());
  }

  private String addFilterLink(Post p) {
    return "";//""<button type='button' class='btn btn-link btn-sm add-filter'>Add Filter</button>";
  }

  private String bookmarkLink(Post p) {
    return String.format(
      "<a href='%s' class='add-bookmark btn btn-link btn-sm' role='button'>Bookmark</a>",
      bookmarkUrl(p.getId()));
  }
  private String removeBookmarkLink(Post p) {
    return String.format(
      "<a href='%s' class='remove-bookmark btn btn-link btn-sm' role='button'>Remove bookmark</a>",
      removeBookmarkUrl(p.getId()));
  }

  private boolean isBookmarked(Post p) {
    return bookmarked == null || bookmarked.contains(p.getId());
  }

  private String markReadLink() {
    int unread = unreadCount();
    if (unread == 0) {
      return "";
    } else {
      return String.format("<a href='%s'><small>Mark %d read</small></a>",
        markReadUrl(), unread);
    }
  }

  private int unreadCount() {
    int unread = 0;
    for (Post p : posts) {
      if (!p.isRead()) {
        unread++;
      }
    }
    return unread;
  }

  private String markReadUrl() {
    StringBuilder ids = new StringBuilder(posts.size() * 10);
    for (Post p : posts) {
      if (ids.length() > 0) {
        ids.append(',');
      }
      ids.append(p.getId());
    }
    return "/markread?posts=" + ids.toString();
  }

  private String bookmarkUrl(long id) {
    return "/add-bookmark?post=" + id;
  }

  private String removeBookmarkUrl(long id) {
    return "/remove-bookmark?post=" + id;
  }

  private String header() {
    if (unreadCount > 0) {
      return Pages.headerTemplate(pageName, String.format(
        "%d of %d posts - Fiets", posts.size(), unreadCount),
        unreadCount, bookmarked.size());
    } else {
      return Pages.headerTemplate(pageName,
        String.format("%d posts - Fiets", posts.size()),
        unreadCount, bookmarked.size());
    }
  }

}
