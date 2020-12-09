package fiets;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fiets.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXParseException;

import fiets.db.Database;
import fiets.db.FeedDao;
import fiets.db.FilterDao;
import fiets.db.PostDao;
import fiets.processors.Process;
import jodd.http.HttpException;

public class FeedService {
  private static final Logger log = LogManager.getLogger();
  private final FeedDao fed;
  private final FilterDao fid;
  private final PostDao pd;

  public FeedService(Database theDb)
          throws SQLException {
    fed = new FeedDao(theDb);
    fid = new FilterDao(theDb, fed);
    pd = new PostDao(theDb);
  }

  public List<Feed> addFeeds(List<String> urls)
          throws Exception {
    List<Feed> feeds = new ArrayList<>();
    for (String url : urls) {
      log.info("Analysing feed {}.", url);
      try {
        Feed feed = new Feed(url, null, null);
        String title = Process.parseTitle(feed);
        feed = new Feed(url, title, null);
        feed = fed.saveFeed(feed);
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
      feeds = fed.getAllFeeds();
      updateFeedPosts(feeds);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateFeedPosts(List<Feed> feeds) throws SQLException {
    List<Filter> allFilters = fid.getAllFilters();
    Filterer ff = new Filterer(allFilters);
    for (Feed feed : feeds) {
      try {
        pd.savePosts(Process.parsePosts(feed, ff), feed);
        fed.touchFeed(feed, "OK");
      } catch (Exception e) {
        fed.touchFeed(feed, e.getMessage());
        log.error("Could not update posts for {}.", feed.getLocation(), e);
      }
    }
    fid.updateMatchCounts(allFilters);
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
    return fed.getAllFeedInfos();
  }

  public int getBookmarksCount() throws SQLException {
    return pd.getBookmarksCount();
  }

  public Feed getFeed(long feedId) throws SQLException {
    return fed.getFeed(feedId).get();
  }

  public void deleteFeed(long feedId) throws SQLException {
    fed.deleteFeed(feedId);
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
    return fed.getAllFeeds();
  }

  public List<Filter> getAllFilters() throws SQLException {
    return fid.getAllFilters();
  }

  public long lastFeedUpdate() throws SQLException {
    return fed.lastFeedUpdate();
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
  }

  public void markPostUnread(long id) throws SQLException {
    pd.markPostUnread(id);
  }

  public void markAllRead(Date before) throws SQLException {
    pd.markAllRead(before);
  }

  public void addFilter(String url, FilterMatch urlMatch, String title, FilterMatch titleMatch) throws SQLException {
    Filter filter = new Filter(0L, url, urlMatch, title, titleMatch, 0L);
    fid.saveFilter(filter);
  }
  
  public void updateFilter(long id, String url, FilterMatch urlMatch, String title, FilterMatch titleMatch) throws SQLException {
    Filter filter = new Filter(id, url, urlMatch, title, titleMatch, 0L);
    fid.updateFilterKeepMatchCount(filter);
  }
  
  public void deleteFilter(long filterId) throws SQLException {
    fid.deleteFilter(filterId);
  }

}
