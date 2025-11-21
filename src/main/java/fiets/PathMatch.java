package fiets;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fiets.model.*;
import fiets.views.*;
import fiets.views.Pages.Name;
import jodd.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum PathMatch {
  showUnreadPosts("") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs) 
      throws SQLException {
      List<Post> posts = fs.getUnreadPosts(sd.intParamOr("num", 20));
      Set<Long> bookmarks = fs.getBookmarks();
      int allCount = fs.getUnreadCount();
      return new PostsHtmlView(
        Name.unread, posts, bookmarks, allCount);
    }
  },
  outdatedCount("outdated-count") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs) {
      int count = fs.getOutdatedCount();
      return new JsonView(new JsonObject().put("outdated", count));
    }
  },
  showReadPosts("show-read") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs) throws SQLException {
      List<Post> posts = fs.getReadPosts(sd.intParamOr("num", 20));
      Set<Long> bookmarks = fs.getBookmarks();
      int allCount = fs.getUnreadCount();
      return new PostsHtmlView(
        Name.read, posts, bookmarks, allCount);
    }
  },
  markPostRead("markread") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs) 
        throws SQLException {
      fs.markPostsRead(sd.longParams("posts"));
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  showBookmarks("bookmarks") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs) {
      List<Post> posts = Collections.emptyList();
      Set<Long> bookmarkedIds = Collections.emptySet();
      try {
        posts = fs.getBookmarkedPosts();
        bookmarkedIds = Server.getIds(posts);
      } catch (SQLException e) {
        log.warn("Could not load bookmarked posts.", e);
      }
      int unreadCount = -1;
      try {
        unreadCount = fs.getUnreadCount();
      } catch (SQLException e) {
        log.warn("Could not load unread count for bookmarks page.", e);
      }
      return new PostsHtmlView(Name.bookmarks, posts, bookmarkedIds, unreadCount);
    }
  },
  addBookmark("add-bookmark") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs) 
        throws SQLException {
      fs.bookmarkPost(sd.intParam("post"));
      return new JsonView(Server.jsonOk());
    }
  },
  removeBookmark("remove-bookmark") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs) 
        throws SQLException {
      fs.removeBookmarkPost(sd.intParam("post"));
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  showFeeds("feeds") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs) 
        throws SQLException {
      List<FeedInfo> feeds = fs.getAllFeedInfos();
      return new FeedsHtmlView(
        sd.getHostname(), feeds, fs.getUnreadCount(), fs.getBookmarksCount());
    }
  },
  showFilters("filters") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs)
        throws SQLException {
      List<Filter> filters = fs.getAllFilters();
      return new FiltersHtmlView(
          sd.getHostname(), filters, fs.getUnreadCount(), fs.getBookmarksCount()
      );
    }
  },
  addFeed("add-feed") {
    @Override public View<?> serve(SessionDecorator sd, FeedService fs) 
        throws Exception {
      List<String> urls = sd.stringParams("url");
      String callback = sd.stringParam("callback");
      List<Feed> added = fs.addFeeds(urls);
      fs.updateFeedPosts(added);
      if (callback == null) {
        return new RedirectView(PathMatch.showFeeds);
      } else {
        return new JavaScriptView(
          String.format("%s(%s)", callback, Server.jsonOk()));
      }
    }
  },
  updateFeed("update-feed") {
    @Override public View<PathMatch> serve(
      SessionDecorator sd, FeedService fs) 
      throws SQLException {
      Feed feed = fs.getFeed(sd.longParam("id"));
      fs.updateFeedPosts(
        Collections.singletonList(feed));
      return new RedirectView(PathMatch.showFeeds);
    }
  },
  deleteFeed("delete-feed") {
    @Override public View<PathMatch> serve(
      SessionDecorator sd, FeedService fs) 
      throws SQLException {
      fs.deleteFeed(sd.longParam("id"));
      return new RedirectView(PathMatch.showFeeds);
    }
  },
  updatePosts("update") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs) {
      fs.updateAllPosts();
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  feverApi("fever") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs)
      throws Exception {
      return new FeverApi().serve(sd, fs);
    }
  },
  counts("counts") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs)
      throws SQLException {
      return new JsonView(
        new JsonObject()
        .put("unread_count", fs.getUnreadCount())
        .put("full_count", fs.getFullCount())
      );
    }
  },
  staticFile("static") {
    @Override public View<InputStream> serve(
      SessionDecorator sd, FeedService fs) 
      throws FileNotFoundException {
      return new FileView(sd);
    }
  },
  addFilter("add-filter") {
    @Override public View<?> serve(SessionDecorator sd, FeedService fs)
            throws Exception {
      Map<String, List<String>> post = sd.postParameters();
      String url = post.get("url").get(0);
      FilterMatch urlMatch = FilterMatch.valueOf(post.get("urlMatch").get(0));
      String title = post.get("title").get(0);
      FilterMatch titleMatch = FilterMatch.valueOf(post.get("titleMatch").get(0));
      fs.addFilter(url, urlMatch, title, titleMatch);
      return new JsonView(Server.jsonOk());
    }
  },
  editFilter("edit-filter") {
    @Override public View<?> serve(SessionDecorator sd, FeedService fs)
            throws Exception {
      Map<String, List<String>> post = sd.postParameters();
      Long id = Long.parseLong(post.get("id").get(0));
      String url = post.get("url").get(0);
      FilterMatch urlMatch = FilterMatch.valueOf(post.get("urlMatch").get(0));
      String title = post.get("title").get(0);
      FilterMatch titleMatch = FilterMatch.valueOf(post.get("titleMatch").get(0));
      fs.updateFilter(id, url, urlMatch, title, titleMatch);
      return new JsonView(Server.jsonOk());
    }
  },
  deleteFilter("delete-filter") {
    @Override public View<PathMatch> serve(
      SessionDecorator sd, FeedService fs) 
      throws SQLException {
      fs.deleteFilter(sd.longParam("id"));
      return new RedirectView(PathMatch.showFilters);
    }
  };

  private static final Logger log = LogManager.getLogger();

  private String base;

  PathMatch(String theBase) {
    base = theBase;
  }

  public String getUrl() {
    return base + '/';
  }

  public abstract View<?> serve(
    SessionDecorator sd, FeedService fs)
    throws Exception;

  public static PathMatch match(SessionDecorator sd) {
    String path = sd.getMainPath();
    if (path == null || path.isEmpty()) {
      return showUnreadPosts;
    }
    for (PathMatch pm : values()) {
      if (path.equals(pm.base)) {
        return pm;
      }
    }
    log.warn("Unknown path '{}', serving unread posts.", sd.getMainPath());
    return showUnreadPosts;
  }
}
