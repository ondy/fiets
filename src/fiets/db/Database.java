package fiets.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Simple database wrapper that provides a singleton H2 connection and some
 * (static) convenience methods.
 */
public class Database implements AutoCloseable {

  private final Connection conn;

  /**
   * Constructor. Initializes the internal connection singleton.
   */
  public Database() throws SQLException {
    conn = DriverManager.getConnection("jdbc:h2:./fiets", "sa", "");
  }

  /**
   * @return connection singleton
   */
  public Connection getConnection() {
    return conn;
  }

  @Override public void close() throws SQLException {
    conn.close();
  }

  /**
   * Get generated key that a (n insert) statement generated.
   */
  public static long getGeneratedKey(Statement st) throws SQLException {
    try (ResultSet rs = st.getGeneratedKeys()) {
      rs.next();
      return rs.getLong(1);
    }
  }

  /**
   * Execute query and return <code>true</code> if there are any results.
   */
  public static boolean hasResult(PreparedStatement ps) throws SQLException {
    try (ResultSet rs = ps.executeQuery()) {
      return rs.next();
    }
  }

  /**
   * Convert {@link Date} to {@link Timestamp}.
   * If <code>date</code> is <code>null</code>, it returns <code>null</code>.
   */
  public static Timestamp toTimestamp(Date date) {
    if (date == null) {
      return null;
    }
    return new Timestamp(date.getTime());
  }
}
