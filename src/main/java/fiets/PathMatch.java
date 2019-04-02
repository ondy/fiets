package fiets;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fiets.display.PostDisplayProvider;
import fiets.model.Feed;
import fiets.model.FeedInfo;
import fiets.model.Post;
import fiets.views.FeedsHtmlView;
import fiets.views.FileView;
import fiets.views.JavaScriptView;
import fiets.views.JsonView;
import fiets.views.PostsHtmlView;
import fiets.views.RedirectView;
import fiets.views.View;
import fiets.views.Pages.Name;
import jodd.json.JsonObject;

public enum PathMatch {
  showUnreadPosts("") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws SQLException {
      List<Post> posts = fs.getUnreadPosts(sd.intParamOr("num", 20));
      Set<Long> bookmarks = fs.getBookmarks();
      int allCount = fs.getUnreadCount();
      return new PostsHtmlView(
        Name.unread, posts, bookmarks, allCount, pdp);
    }
  },
  outdatedCount("outdated-count") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws SQLException {
      int count = fs.getOutdatedCount();
      return new JsonView(new JsonObject().put("outdated", count));
    }
  },
  showReadPosts("show-read") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws SQLException {
      List<Post> posts = fs.getReadPosts(sd.intParamOr("num", 20));
      Set<Long> bookmarks = fs.getBookmarks();
      int allCount = fs.getUnreadCount();
      return new PostsHtmlView(
        Name.read, posts, bookmarks, allCount, pdp);
    }
  },
  markPostRead("markread") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException {
      fs.markPostsRead(sd.longParams("posts"));
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  showBookmarks("bookmarks") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException {
      List<Post> posts = fs.getBookmarkedPosts();
      return new PostsHtmlView(
        Name.bookmarks, posts, Server.getIds(posts), fs.getUnreadCount(), pdp);
    }
  },
  addBookmark("add-bookmark") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException {
      fs.bookmarkPost(sd.intParam("post"));
      return new JsonView(Server.jsonOk());
    }
  },
  removeBookmark("remove-bookmark") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException {
      fs.removeBookmarkPost(sd.intParam("post"));
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  showFeeds("feeds") {
    @Override public View<String> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException {
      List<FeedInfo> feeds = fs.getAllFeedInfos();
      return new FeedsHtmlView(
        feeds, fs.getUnreadCount(), fs.getBookmarksCount());
    }
  },
  addFeed("add-feed") {
    @Override public View<?> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws SQLException, Exception {
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
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws SQLException {
      Feed feed = fs.getFeed(sd.longParam("id"));
      fs.updateFeedPosts(
        Collections.singletonList(feed));
      return new RedirectView(PathMatch.showFeeds);
    }
  },
  deleteFeed("delete-feed") {
    @Override public View<PathMatch> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws SQLException {
      fs.deleteFeed(sd.longParam("id"));
      return new RedirectView(PathMatch.showFeeds);
    }
  },
  updatePosts("update") {
    @Override public View<PathMatch> serve(SessionDecorator sd, FeedService fs,
      PostDisplayProvider pdp) throws Exception {
      fs.updateAllPosts();
      return new RedirectView(PathMatch.showUnreadPosts);
    }
  },
  feverApi("fever") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp)
      throws Exception {
      return new FeverApi().serve(sd, fs);
    }
  },
  counts("counts") {
    @Override public View<String> serve(
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp)
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
      SessionDecorator sd, FeedService fs, PostDisplayProvider pdp) 
      throws FileNotFoundException {
      return new FileView(sd);
    }
  }
  ;

  private String base;

  private PathMatch(String theBase) {
    base = theBase;
  }

  public String getUrl() {
    return base + '/';
  }

  public abstract View<?> serve(
    SessionDecorator sd, FeedService fs, PostDisplayProvider pdp)
    throws Exception;

  public static PathMatch match(SessionDecorator sd) {
    String path = sd.getMainPath();
    for (PathMatch pm : values()) {
      if (path.equals(pm.base)) {
        return pm;
      }
    }
    throw new IllegalArgumentException("Path not found: " + sd.getMainPath());
  }
}