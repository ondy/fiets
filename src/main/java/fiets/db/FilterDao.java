package fiets.db;

import fiets.model.Feed;
import fiets.model.Filter;
import fiets.model.FilterMatch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilterDao {

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
        + "feed BIGINT,"
        + "url VARCHAR(2048),"
        + "urlmatch TINYINT,"
        + "title VARCHAR(2048),"
        + "titlematch TINYINT"
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

  public List<Filter> getAllFilters() throws SQLException {
    List<Filter> filters = new ArrayList<>();
    try (PreparedStatement ps = db.getConnection().prepareStatement(
      "SELECT feed, url, urlmatch, title, titlematch FROM filter")) {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        filters.add(parseFilterResultSet(rs));
      }
    }
    return filters;
  }

  private Filter parseFilterResultSet(ResultSet rs) throws SQLException {
    int i = 0;
    long feedId = rs.getLong(++i);
    String url = rs.getString(++i);
    FilterMatch urlMatch = FilterMatch.values()[(rs.getInt(++i))];
    String title = rs.getString(++i);
    FilterMatch titleMatch = FilterMatch.values()[rs.getInt(++i)];
    Optional<Feed> feed = fd.getFeed(feedId);
    Filter filter = new Filter(url, urlMatch, title, titleMatch);
    return filter;
  }
}
