package fiets.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.model.Feed;
import fiets.model.FeedInfo;

/**
 * DAO for the feeds table.
 */
public class FeedDao {

  private static final Logger log = LogManager.getLogger();
  private Database db;

  public FeedDao(Database theDb) throws SQLException {
    db = theDb;
    createTable();
    createIndexes();
  }

  private int createTable() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "CREATE TABLE IF NOT EXISTS feed ("
        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
        + "lastAccess DATETIME,"
        + "lastStatus VARCHAR(1024),"
        + "location VARCHAR(2048),"
        + "title VARCHAR(1024)"
      + ");")) {
      return ps.executeUpdate();
    }
  }

  private void createIndex(String column) throws SQLException {
    db.createIndexIfNotExists("feed", column);
  }

  private void createIndexes() throws SQLException {
    createIndex("id");
    createIndex("location");
  }

  public Feed saveFeed(Feed feed) throws SQLException {
    if (feed.getId() == 0) {
      if (existsFeed(feed.getLocation())) {
        updateFeedByLocation(feed);
      } else {
        return insertFeed(feed);
      }
    } else {
      updateFeedById(feed);
    }
    return feed;
  }

  public void touchFeed(Feed feed, String status) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "UPDATE feed SET lastAccess=?,lastStatus=? WHERE id=?")) {
      int i = 0;
      ps.setTimestamp(++i, Database.toTimestamp(new Date()));
      ps.setString(++i, status);
      ps.setLong(++i, feed.getId());
      ps.executeUpdate();
    }
  }

  public long lastFeedUpdate() throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT MAX(lastAccess) FROM feed")) {
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getTimestamp(1).getTime();
        } else {
          return 0l;
        }
      }
    }
  }

  private void updateFeedById(Feed feed) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "UPDATE feed SET location=?,title=?,lastAccess=?"
      + "WHERE id=?")) {
      ps.setString(1, feed.getLocation());
      ps.setString(2, feed.getTitle());
      ps.setTimestamp(3, Database.toTimestamp(feed.getLastAccess()));
      ps.setLong(4, feed.getId());
      ps.executeUpdate();
    }
    log.debug("Updated feed {} with ID {}.", feed.getLocation(), feed.getId());
  }

  private void updateFeedByLocation(Feed feed) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "UPDATE feed SET lastAccess=? WHERE location=?")) {
      ps.setTimestamp(1, Database.toTimestamp(feed.getLastAccess()));
      ps.setString(2, feed.getLocation());
      ps.executeUpdate();
    }
    log.debug("Updated feed {} with ID {}.", feed.getLocation(), feed.getId());
  }

  private boolean existsFeed(String location) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
        "SELECT * FROM feed WHERE location=?")) {
      ps.setString(1, location);
      return Database.hasResult(ps);
    }
  }

  private Feed insertFeed(Feed feed) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "INSERT INTO feed (location, title, lastAccess, lastStatus) "
      + "VALUES (?,?,?,?)",
      Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, feed.getLocation());
      ps.setString(2, feed.getTitle());
      ps.setTimestamp(3, Database.toTimestamp(feed.getLastAccess()));
      ps.setString(4, feed.getLastStatus());
      ps.executeUpdate();
      feed = new Feed(Database.getGeneratedKey(ps),
        feed.getLocation(), feed.getTitle(), feed.getLastAccess(),
        feed.getLastStatus());
    }
    log.debug("Inserted feed {} with ID {}", feed.getLocation(), feed.getId());
    return feed;
  }

  public Optional<Feed> getFeed(long id) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT id,location,title,lastAccess,lastStatus FROM feed WHERE id=?")) {
      ps.setLong(1, id);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(parseFeedResultSet(rs));
      } else {
        return Optional.empty();
      }
    }
  }

  public List<Feed> getAllFeeds() throws SQLException {
    List<Feed> feeds = new ArrayList<>();
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT id,location,title,lastAccess,lastStatus FROM feed ORDER BY title ASC")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        feeds.add(parseFeedResultSet(rs));
      }
    }
    return feeds;
  }

  public static void main(String[] args) throws SQLException {
    try (Database db = new Database()) {
      FeedDao fd = new FeedDao(db);
      System.out.println(fd.getAllFeedInfos().size());
    }

  }
  public List<FeedInfo> getAllFeedInfos() throws SQLException {
    List<FeedInfo> feeds = new ArrayList<>();
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT DISTINCT(feed.id),feed.location,feed.title,feed.lastAccess,feed.lastStatus,"
      + "COUNT(CASE WHEN post.read=0 THEN 1 END),"
      + "COUNT(CASE WHEN post.read=1 THEN 1 END),"
      + "MAX(post.date) "
      + "FROM feed "
      + "LEFT JOIN postfeed ON feed.id=postfeed.feed "
      + "LEFT JOIN post ON postfeed.post=post.id "
      + "GROUP BY feed.id ORDER BY feed.id")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        feeds.add(parseFeedInfoResultSet(rs));
      }
    }
    return feeds;
  }

  private static Feed parseFeedResultSet(ResultSet rs) throws SQLException {
    return parseFeedResultSet(rs, new int[] {0});
  }
  private static Feed parseFeedResultSet(ResultSet rs, int[] ctrRef) throws SQLException {
    int i = ctrRef[0];
    long id = rs.getLong(++i);
    String location = rs.getString(++i);
    String title = rs.getString(++i);
    Date lastAccess = rs.getTimestamp(++i);
    String lastStatus = rs.getString(++i);
    if (lastStatus == null) {
      lastStatus = "unknown";
    }
    ctrRef[0] = i;
    Feed feed = new Feed(id, location, title, lastAccess, lastStatus);
    return feed;
  }

  private static FeedInfo parseFeedInfoResultSet(ResultSet rs) throws SQLException {
    int[] ctrRef = new int[] {0};
    Feed f = parseFeedResultSet(rs, ctrRef);
    int i = ctrRef[0];
    int unread = rs.getInt(++i);
    int read = rs.getInt(++i);
    Date mostRecent = rs.getTimestamp(++i);
    return new FeedInfo(f, unread, read, mostRecent);
  }

  public void deleteFeed(long id) throws SQLException {
    Feed f = getFeed(id).get();
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "DELETE FROM feed WHERE id=?")) {
      ps.setLong(1, id);
      ps.executeUpdate();
    }
    log.debug("Deleted feed {} with ID {}.", f.getLocation(), id);
  }
}
