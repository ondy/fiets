package fiets.views;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import fiets.model.Post;
import fiets.views.Pages.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostsHtmlView implements View<String> {

  private static final Logger log = LogManager.getLogger();
  private static final int DEFAULT_PAGE_SIZE = 20;

  private final List<Post> posts;
  private final int unreadCount;
  private final Set<Long> bookmarked;
  private Name pageName;

  public PostsHtmlView(
    Name thePageName,
    List<Post> thePosts, Set<Long> theBookmarked, int theUnreadCount) {
    pageName = thePageName;
    posts = thePosts == null ? Collections.emptyList() : thePosts;
    bookmarked = theBookmarked == null ? Collections.emptySet() : theBookmarked;
    unreadCount = theUnreadCount;
  }

  @Override public String getMimeType() {
    return "text/html";
  }

  @Override public String getContent() {
    StringBuilder sb = new StringBuilder()
      .append(header())
      .append(String.format(
        "<ul class='list-group posts-list' data-page-size='%d'>",
        DEFAULT_PAGE_SIZE));
    for (Post p : posts) {
      try {
        sb.append(post(p));
      } catch (RuntimeException e) {
        log.warn(String.format("Could not render post %s", safePostId(p)), e);
        sb.append(errorPost(p));
      }
    }
    return sb.append("</ul>")
            .append(Pages.editFilterTemplate())
            .append(Pages.footerTemplate(markReadLink()))
            .toString();
  }

  private static String safePostId(Post p) {
    if (p == null) {
      return "<unknown>";
    }
    return String.valueOf(p.getId());
  }

  private String post(Post p) {
    PostDisplay display = new PostDisplay(p);
    return String.format(
      "<li class='list-group-item post %s' data-post-id='%d' data-read='%s'>"
      + "<div class='post-meta'>"
      + "<small class='post-date'>%s</small>"
      + "<span class='meta-separator'>|</span>"
      + "<small class='feed-title-full'>%s</small>"
      + "<small class='feed-title-mobile'>%s</small>"
      + "<span class='post-actions'>"
      + addFilterLink(p)
      + bookmarkLink(p)
      + removeBookmarkLink(p)
      + "</span>"
      + "</div>"
      + "<h3 title='%s'><a href='%s' target='_blank'>%s</a></h3>"
      + "<div>%s</div></li>",
      isBookmarked(p) ? "bookmarked" : "",
      p.getId(),
      p.isRead(),
      display.getDate(), display.getFeedTitle(), display.getMobileFeedTitle(),
      display.getTitle(), p.getLocation(),
      display.getShortenedTitle(),
      display.getShortenedSnippet());
  }

  private String addFilterLink(Post p) {
    return "<button type='button' class='btn btn-link btn-sm add-filter'>+Filter</button>";
  }

  private String bookmarkLink(Post p) {
    return String.format(
      "<a href='%s' class='add-bookmark btn btn-link btn-sm' role='button'>"
      + "<span class='label-full'>+Bookmark</span><span class='label-short'>+Mark</span></a>",
      bookmarkUrl(p.getId()));
  }
  private String removeBookmarkLink(Post p) {
    return String.format(
      "<a href='%s' class='remove-bookmark btn btn-link btn-sm' role='button'>"
      + "<span class='label-full'>-Bookmark</span><span class='label-short'>-Mark</span></a>",
      removeBookmarkUrl(p.getId()));
  }

  private boolean isBookmarked(Post p) {
    return bookmarked == null || bookmarked.contains(p.getId());
  }

  private String markReadLink() {
    if (pageName == Name.bookmarks) {
      return "";
    }
    int unread = unreadCount();
    if (unread == 0) {
      return "";
    } else {
      List<Post> postsToMark = unreadPostsToShow();
      return String.format(
        "<a class='mark-read-action' data-total-unread='%d' href='%s'>"
        + "<small>Mark %d of %d read</small></a>",
        unreadCount, markReadUrl(postsToMark), postsToMark.size(), unreadCount);
    }
  }

  private int unreadCount() {
    return (int) posts.stream().filter(p -> !p.isRead()).count();
  }

  private String markReadUrl(List<Post> postsToMark) {
    StringBuilder ids = new StringBuilder(postsToMark.size() * 10);
    for (Post p : postsToMark) {
      if (ids.length() > 0) {
        ids.append(',');
      }
      ids.append(p.getId());
    }
    return "/markread?posts=" + ids;
  }

  private String bookmarkUrl(long id) {
    return "/add-bookmark?post=" + id;
  }

  private String removeBookmarkUrl(long id) {
    return "/remove-bookmark?post=" + id;
  }

  private String errorPost(Post p) {
    boolean bookmarkedPost = p != null && isBookmarked(p);
    String classes = String.format(
      "list-group-item post error%s",
      bookmarkedPost ? " bookmarked" : "");
    StringBuilder builder = new StringBuilder(String.format(
      "<li class='%s'><small>Could not render post %s</small>",
      classes, safePostId(p)));
    if (bookmarkedPost) {
      builder.append(" <span class='post-actions'>")
             .append(removeBookmarkLink(p))
             .append("</span>");
    }
    return builder.append("</li>").toString();
  }

  private String header() {
    if (unreadCount > 0) {
      return Pages.headerTemplate(pageName, String.format(
        "%d of %d posts - Fiets", displayedPostsCount(), unreadCount),
        unreadCount, bookmarked.size());
    } else {
      return Pages.headerTemplate(pageName,
        String.format("%d posts - Fiets", displayedPostsCount()),
        unreadCount, bookmarked.size());
    }
  }

  private int displayedPostsCount() {
    return Math.min(posts.size(), DEFAULT_PAGE_SIZE);
  }

  private List<Post> unreadPostsToShow() {
    return posts.stream()
      .filter(p -> !p.isRead())
      .limit(DEFAULT_PAGE_SIZE)
      .toList();
  }

}
