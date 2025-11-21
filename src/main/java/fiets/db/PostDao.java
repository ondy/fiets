package fiets.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.model.Feed;
import fiets.model.Post;

/**
 * A post is considered equal to an existing one when either the location or the
 * title matches. Only when both the location and the title differ is a post
 * treated as new.
 */
public class PostDao {

  private static final String OUTDATED_SPEC =
    " WHERE post.read = true AND "
    + "post.lastaccess <= DATEADD('month', -1, current_timestamp()) AND "
    + "post.id NOT IN (SELECT bookmarkedpost.post FROM bookmarkedpost)";
  private static final Logger log = LogManager.getLogger();

  private Database db;

  public PostDao(Database theDb) throws SQLException {
    db = theDb;
    createTables();
    createIndexes();
    upgradeTables();
  }

  private void upgradeTables() throws SQLException {
    addLastaccessColumn();
  }

  public void createTables() throws SQLException {
    createPostTable();
    createPostFeedTable();
    createBookmarkTable();
  }

  private void createIndex(String column) throws SQLException {
    db.createIndexIfNotExists("post", column);
  }

  private void createIndexes() throws SQLException {
    createIndex("id");
    createIndex("date");
    createIndex("read");
    createIndex("location");
  }

  public Post savePost(Post post, Feed feed) throws SQLException {
    if (post.getId() == 0L) {
      Post existing = loadPostByLocation(post.getLocation());
      if (existing == null) {
        existing = loadPostByTitle(post.getTitle());
      }
      if (existing == null) {
        post = insertPost(post);
      } else {
        post = existing;
        log.debug("Post {} already exists with ID {}.",
          post.getLocation(), post.getId());
        touchPost(post);
      }
    } else {
      updatePostById(post);
    }
    savePostFeed(post, feed);
    return post;
  }

  private Post loadPostByTitle(String title) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      selectPost("WHERE post.title=?"))) {
      ps.setString(1, title);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return parsePostResultSet(rs);
      }
    }
  }

  public void savePosts(List<Post> posts, Feed feed) throws SQLException {
    for (Post post : posts) {
      savePost(post, feed);
    }
  }

  public Post loadPostByLocation(String location) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      selectPost("WHERE post.location=?"))) {
      ps.setString(1, location);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return parsePostResultSet(rs);
      }
    }
  }

  public Set<Long> getBookmarks() throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT post FROM bookmarkedpost")) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Long> posts = new ArrayList<>();
        while (rs.next()) {
          posts.add(rs.getLong(1));
        }
        return new HashSet<>(posts);
      }
    }
  }

  public int getUnreadCount() throws SQLException {
    return getCount(" WHERE post.read=false");
  }

  public int getReadCount() throws SQLException {
    return getCount(" WHERE post.read=true");
  }

  public int getOutdatedCount() throws SQLException {
    return getCount(OUTDATED_SPEC);
  }

  public void deleteOutdated() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "DELETE FROM post" + OUTDATED_SPEC)) {
      ps.executeUpdate();
    }
  }

  public void deletePost(long id) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
            "DELETE FROM post WHERE id=?")) {
      ps.setLong(1, id);
      ps.executeUpdate();
    }
  }

  public void deletePostsOfFeed(long feedId) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
            "SELECT post.id FROM post " +
            "INNER JOIN postfeed ON post.id=postfeed.post " +
            "WHERE postfeed.feed=?")) {
      ps.setLong(1, feedId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Post> posts = new ArrayList<>();
        while (rs.next()) {
          long id = rs.getLong(1);
          deletePost(id);
        }
      }
    }

    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "DELETE FROM postfeed WHERE postfeed.feed=?")) {
      ps.setLong(1, feedId);
      ps.executeUpdate();
    }
  }

  public int getFullCount() throws SQLException {
    return getCount("");
  }

  public int getBookmarksCount() throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT COUNT(post) FROM bookmarkedpost")) {
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private int getCount(String appendix) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT COUNT(id) FROM post" + appendix)) {
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  public List<Post> getUnreadPosts(int num) throws SQLException {
    Connection conn = db.getConnection();
    String appendix = "WHERE post.read=false ORDER BY post.date ASC";
    if (num > 0) {
      appendix += " LIMIT 0," + num;
    }
    return loadPosts(conn, appendix);
  }

  public List<Post> getReadPosts(int num) throws SQLException {
    Connection conn = db.getConnection();
    String appendix = "WHERE post.read=true ORDER BY post.date DESC";
    if (num > 0) {
      appendix += " LIMIT 0," + num;
    }
    return loadPosts(conn, appendix);
  }

  private List<Post> loadPosts(Connection conn, String appendix)
    throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
      selectPost(appendix))) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Post> posts = new ArrayList<>();
        while (rs.next()) {
          posts.add(parsePostResultSet(rs));
        }
        return posts;
      }
    }
  }

  public List<Post> postsAfter(long sinceId) throws SQLException {
    Connection conn = db.getConnection();
    String appendix = String.format(
      "WHERE post.id > %d ORDER BY post.id ASC", sinceId);
    return loadPosts(conn, appendix);
  }

  public List<Post> postsBefore(long maxId) throws SQLException {
    Connection conn = db.getConnection();
    String appendix = String.format(
      "WHERE post.id < %d ORDER BY post.id ASC", maxId);
    return loadPosts(conn, appendix);
  }

  public List<Post> posts(List<Long> withIds) throws SQLException {
    String idString = withIds.toString();
    idString = idString.substring(1, idString.length()-1);
    Connection conn = db.getConnection();
    String appendix = String.format(
      "WHERE post.id IN (%s) ORDER BY post.id ASC", idString);
    return loadPosts(conn, appendix);
  }

  public List<Post> getBookmarkedPosts() throws SQLException {
    Connection conn = db.getConnection();
    String appendix =
        "ORDER BY post.date ASC";
    try (PreparedStatement ps = conn.prepareStatement(
      selectBookmarkedPost(appendix))) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Post> posts = new ArrayList<>();
        while (rs.next()) {
          long bookmarkedId = rs.getLong(1);
          posts.add(parsePostResultSet(rs, 1, bookmarkedId));
        }
        return posts;
      }
    }
  }

  private static String selectPost(String appendix) {
    return "SELECT "
      + "post.id,post.date,post.location,post.snippet,post.title,post.read,"
      + "feed.id,feed.location,feed.title,feed.lastAccess,feed.lastStatus "
      + "FROM post "
      + "LEFT JOIN postfeed ON post.id=postfeed.post "
      + "LEFT JOIN feed ON postfeed.feed=feed.id "
      + (appendix == null ? "" : appendix);
  }

  private static String selectBookmarkedPost(String appendix) {
    return "SELECT "
      + "bookmarkedpost.post,"
      + "post.id,post.date,post.location,post.snippet,post.title,post.read,"
      + "feed.id,feed.location,feed.title,feed.lastAccess,feed.lastStatus "
      + "FROM bookmarkedpost "
      + "LEFT JOIN post ON bookmarkedpost.post=post.id "
      + "LEFT JOIN postfeed ON post.id=postfeed.post "
      + "LEFT JOIN feed ON postfeed.feed=feed.id "
      + (appendix == null ? "" : appendix);
  }

  private Post parsePostResultSet(ResultSet rs) throws SQLException {
    return parsePostResultSet(rs, 0, null);
  }

  private Post parsePostResultSet(ResultSet rs, int offset, Long fallbackId)
      throws SQLException {
    int index = offset;
    long id = rs.getLong(++index);
    if (rs.wasNull() && fallbackId != null) {
      id = fallbackId;
    }
    Date date = rs.getTimestamp(++index);
    String location = rs.getString(++index);
    String snippet = rs.getString(++index);
    String title = rs.getString(++index);
    boolean read = rs.getBoolean(++index);
    long feedId = rs.getLong(++index);
    String feedLocation = rs.getString(++index);
    String feedTitle = rs.getString(++index);
    Date feedLastAccess = rs.getTimestamp(++index);
    String feedLastStatus = rs.getString(++index);
    return new Post(id, location, date, title, snippet, read,
      new Feed(feedId, feedLocation, feedTitle, feedLastAccess, feedLastStatus));
  }

  private void updatePostById(Post post) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "UPDATE post "
      + "SET date=?,location=?,snippet=?,title=?,read=?,lastaccess=? "
      + "WHERE id=?")) {
      int index = preparePostStatement(ps, post);
      ps.setLong(++index, post.getId());
      ps.executeUpdate();
    }
    log.debug("Updated post {} with ID {}.", post.getLocation(), post.getId());
  }

  private void touchPost(Post post) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "UPDATE post SET lastaccess=? WHERE id=?")) {
      int index = 0;
      ps.setTimestamp(++index, Database.toTimestamp(new Date()));
      ps.setLong(++index, post.getId());
      ps.executeUpdate();
    }
    log.debug("Touched post {} with ID {}.", post.getLocation(), post.getId());
  }

  private Post insertPost(Post post) throws SQLException {
    Connection conn = db.getConnection();
    log.debug("Insert post " + post.getLocation());
    try (PreparedStatement ps = conn.prepareStatement(
      "INSERT INTO post (date,location,snippet,title,read,lastaccess) "
      + "VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
      preparePostStatement(ps, post);
      ps.executeUpdate();
      post = new Post(Database.getGeneratedKey(ps), post);
    }
    log.debug("Inserted post {} with ID {}.", post.getLocation(), post.getId());
    return post;
  }

  private int preparePostStatement(PreparedStatement ps, Post post)
    throws SQLException {
    int index = 0;
    ps.setTimestamp(++index, Database.toTimestamp(post.getDate()));
    ps.setString(++index, post.getLocation());
    ps.setString(++index, shorten(post.getSnippet(), 4096));
    ps.setString(++index, post.getTitle());
    ps.setBoolean(++index, post.isRead());
    ps.setTimestamp(++index, Database.toTimestamp(new Date()));
    return index;
  }

  private String shorten(String snippet, int len) {
    if (snippet.length() > len) {
      return snippet.substring(0, len);
    }
    return snippet;
  }

  private int createPostTable() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "CREATE TABLE IF NOT EXISTS post ("
        + "id BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,"
        + "date TIMESTAMP,"
        + "location VARCHAR(2048),"
        + "snippet VARCHAR(4096),"
        + "title VARCHAR(1024),"
        + "read TINYINT,"
        + "lastaccess TIMESTAMP"
      + ")")) {
      return ps.executeUpdate();
    }
  }

  private void addLastaccessColumn() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "ALTER TABLE post "
      + "ADD COLUMN IF NOT EXISTS lastaccess TIMESTAMP DEFAULT ?")) {
      ps.setTimestamp(1, Database.toTimestamp(new Date()));
      ps.executeUpdate();
    }
  }

  private int createPostFeedTable() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "CREATE TABLE IF NOT EXISTS postfeed ("
        + "post BIGINT,"
        + "feed BIGINT"
      + ")")) {
      return ps.executeUpdate();
    }
  }

  private int createBookmarkTable() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "CREATE TABLE IF NOT EXISTS bookmarkedpost ("
        + "post BIGINT"
      + ")")) {
      return ps.executeUpdate();
    }
  }

  private boolean existsPostFeed(Post post, Feed feed) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT post, feed FROM postfeed WHERE post=? AND feed=?")) {
      ps.setLong(1, post.getId());
      ps.setLong(2, feed.getId());
      return Database.hasResult(ps);
    }
  }

  private void savePostFeed(Post post, Feed feed) throws SQLException {
    if (!existsPostFeed(post, feed)) {
      try (PreparedStatement ps = db.getConnection().prepareStatement(
        "INSERT INTO postfeed (post, feed) VALUES (?, ?)")) {
        ps.setLong(1, post.getId());
        ps.setLong(2, feed.getId());
        ps.executeUpdate();
      }
    }
  }

  public void markPostsRead(List<Long> postIds) throws SQLException {
    int num = postIds.size();
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "UPDATE post SET post.read=true WHERE post.id IN " + inCondition(num))) {
      int i = 0;
      for (Long postId : postIds) {
        ps.setLong(++i, postId);
      }
      ps.executeUpdate();
    }
  }

  public void markPostRead(long postId) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "UPDATE post SET post.read=true WHERE post.id=?")) {
      ps.setLong(1, postId);
      ps.executeUpdate();
    }
  }

  public void markPostUnread(long postId) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "UPDATE post SET post.read=false WHERE post.id=?")) {
      ps.setLong(1, postId);
      ps.executeUpdate();
    }
  }

  public void bookmarkPost(long postId) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "INSERT INTO bookmarkedpost (post) VALUES (?)")) {
      ps.setLong(1, postId);
      ps.executeUpdate();
    }
  }

  public void removeBookmarkPost(long postId) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "DELETE FROM bookmarkedpost WHERE post=?")) {
      ps.setLong(1, postId);
      ps.executeUpdate();
    }
  }

  private static String inCondition(int num) {
    StringBuilder sb = new StringBuilder(num*3);
    for (int i = 0; i < num; i++) {
      sb.append("?,");
    }
    return '(' + sb.deleteCharAt(sb.length()-1).toString() + ')';
  }

  public void markAllRead(Date before) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "UPDATE post SET post.read=true WHERE post.date < ?")) {
      ps.setTimestamp(1, Database.toTimestamp(before));
      ps.executeUpdate();
    }
  }

}
