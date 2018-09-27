package fiets;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXParseException;

import fiets.db.Database;
import fiets.db.FeedDao;
import fiets.db.PostDao;
import fiets.filter.RawPostFilter;
import fiets.model.Feed;
import fiets.model.FeedInfo;
import fiets.model.Post;
import fiets.processors.Process;
import jodd.http.HttpException;

public class FeedService {
  private static final Logger log = LogManager.getLogger();
  private final FeedDao fd;
  private final PostDao pd;
  private final RawPostFilter filter;

  public FeedService(Database theDb, RawPostFilter theFilter)
    throws SQLException {
    fd = new FeedDao(theDb);
    pd = new PostDao(theDb);
    filter = theFilter;
  }

  public List<Feed> addFeeds(List<String> urls)
    throws SQLException, Exception {
    List<Feed> feeds = new ArrayList<>();
    for (String url : urls) {
      log.info("Analysing feed {}.", url);
      try {
        Feed feed = new Feed(url, null, null);
        String title = Process.parseTitle(feed);
        feed = new Feed(url, title, null);
        feed = fd.saveFeed(feed);
        feeds.add(feed);
      } catch (IllegalArgumentException e) {
        log.error(e, e);
      } catch (SAXParseException e) {
        log.error("Parse error in feed {}, ignoring this.", url);
      } catch (HttpException e) {
        log.error("Could not connect {}, ignoring this.", url);
      }
    }
    return feeds;
  }

  public void updateAllPosts() {
    List<Feed> feeds;
    try {
      feeds = fd.getAllFeeds();
      updateFeedPosts(feeds);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateFeedPosts(List<Feed> feeds) throws SQLException {
    for (Feed feed : feeds) {
      try {
        pd.savePosts(Process.parsePosts(feed, filter), feed);
        fd.touchFeed(feed, "OK");
      } catch (Exception e) {
        fd.touchFeed(feed, e.getMessage());
        log.error("Could not update posts for {}.", feed.getLocation(), e);
      }
    }
  }

  public Set<Long> getBookmarks() throws SQLException {
    return pd.getBookmarks();
  }

  public int getUnreadCount() throws SQLException {
    return pd.getUnreadCount();
  }

  public List<Post> getUnreadPosts(int num) throws SQLException {
    return pd.getUnreadPosts(num);
  }

  public List<Post> getReadPosts(int num) throws SQLException {
    return pd.getReadPosts(num);
  }

  public void markPostsRead(List<Long> postIds) throws SQLException {
    pd.markPostsRead(postIds);
  }

  public List<Post> getBookmarkedPosts() throws SQLException {
    return pd.getBookmarkedPosts();
  }

  public void bookmarkPost(long postId) throws SQLException {
    pd.bookmarkPost(postId);
  }

  public void removeBookmarkPost(long postId) throws SQLException {
    pd.removeBookmarkPost(postId);
  }

  public List<FeedInfo> getAllFeedInfos() throws SQLException {
    return fd.getAllFeedInfos();
  }

  public int getBookmarksCount() throws SQLException {
    return pd.getBookmarksCount();
  }

  public Feed getFeed(long feedId) throws SQLException {
    return fd.getFeed(feedId);
  }

  public void deleteFeed(long feedId) throws SQLException {
    fd.deleteFeed(feedId);
    pd.deletePostsOfFeed(feedId);
  }

  public int getFullCount() throws SQLException {
    return pd.getFullCount();
  }

  public int getOutdatedCount() {
    try {
      return pd.getOutdatedCount();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void dropOutdated() {
    try {
      pd.deleteOutdated();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<Feed> getAllFeeds() throws SQLException {
    return fd.getAllFeeds();
  }

  public long lastFeedUpdate() throws SQLException {
    return fd.lastFeedUpdate();
  }

  public List<Post> postsAfter(long sinceId) throws SQLException {
    return pd.postsAfter(sinceId);
  }

  public List<Post> postsBefore(long maxId) throws SQLException {
    return pd.postsBefore(maxId);
  }

  public List<Post> posts(List<Long> withIds) throws SQLException {
    return pd.posts(withIds);
  }

  public void markPostRead(long id) throws SQLException {
    pd.markPostRead(id);
    // TODO Auto-generated method stub

  }

  public void markPostUnread(long id) throws SQLException {
    pd.markPostUnread(id);
  }

  public void markAllRead(Date before) throws SQLException {
    pd.markAllRead(before);
  }
}
