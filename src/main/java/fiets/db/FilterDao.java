package fiets.db;

import fiets.model.Feed;
import fiets.model.Filter;
import fiets.model.FilterMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilterDao {
  private static final Logger log = LogManager.getLogger();

  private Database db;
  private FeedDao fd;

  public FilterDao(Database theDb, FeedDao theFd) throws SQLException {
    db = theDb;
    fd = theFd;
    createTable();
  }

  private int createTable() throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "CREATE TABLE IF NOT EXISTS filter ("
        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
        + "feed BIGINT,"
        + "url VARCHAR(2048),"
        + "urlmatch TINYINT,"
        + "title VARCHAR(2048),"
        + "titlematch TINYINT,"
        + "matchcount BIGINT DEFAULT 0"
      + ");")) {
      return ps.executeUpdate();
    }
  }

  public Filter saveFilter(Filter filter) throws SQLException {
    return insertFilter(filter);
  }

  private Filter insertFilter(Filter filter) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "INSERT INTO filter (url, urlmatch, title, titlematch) "
      + "VALUES (?,?,?,?)")) {
      ps.setString(1, filter.getUrl());
      ps.setInt(2, filter.getUrlMatch().ordinal());
      ps.setString(3, filter.getTitle());
      ps.setInt(4, filter.getTitleMatch().ordinal());
      ps.executeUpdate();
    }
    return filter;  
  }

  public Filter updateFilterKeepMatchCount(Filter filter) throws SQLException {
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "UPDATE filter SET url=?, urlMatch=?, title=?, titleMatch=? WHERE id=?")) {
      ps.setString(1, filter.getUrl());
      ps.setInt(2, filter.getUrlMatch().ordinal());
      ps.setString(3, filter.getTitle());
      ps.setInt(4, filter.getTitleMatch().ordinal());
      ps.setLong(5, filter.getId());
      ps.executeUpdate();
    }
    return filter;  
  }


  
  public List<Filter> getAllFilters() throws SQLException {
    List<Filter> filters = new ArrayList<>();
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT id, url, urlmatch, title, titlematch, matchcount FROM filter")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        filters.add(parseFilterResultSet(rs));
      }
    }
    return filters;
  }

  private Filter parseFilterResultSet(ResultSet rs) throws SQLException {
    int i = 0;
    long id = rs.getLong(++i);
    String url = rs.getString(++i);
    FilterMatch urlMatch = FilterMatch.values()[(rs.getInt(++i))];
    String title = rs.getString(++i);
    FilterMatch titleMatch = FilterMatch.values()[rs.getInt(++i)];
    long matchCount = rs.getLong(++i);
    Filter filter = new Filter(id, url, urlMatch, title, titleMatch, matchCount);
    return filter;
  }
  
  public void deleteFilter(long id) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
      "DELETE FROM filter WHERE id=?")) {
      ps.setLong(1, id);
      ps.executeUpdate();
    }
    log.info("Deleted filter with ID {}.", id);
  }

  public void updateMatchCount(Filter f) throws SQLException {
    Connection conn = db.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE filter SET matchcount=? WHERE id=?")) {
      ps.setLong(1, f.getMatchCount());
      ps.setLong(2, f.getId());
      ps.executeUpdate();
    }
    log.debug("Increased match count for filter with ID {} to {}.", f.getId(), f.getMatchCount());
  }

  public void updateMatchCounts(List<Filter> filters) throws SQLException {
    for (Filter filter : filters) {
      updateMatchCount(filter);
    }
  }
}
