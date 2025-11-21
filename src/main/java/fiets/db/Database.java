package fiets.db;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final String DB_BASE = "./db/fiets";
  private static final String DB_URL =
    "jdbc:h2:" + DB_BASE + ";MODE=LEGACY;DATABASE_TO_LOWER=TRUE";
  private static final Logger log = LogManager.getLogger();

  private final Connection conn;

  /**
   * Constructor. Initializes the internal connection singleton.
   */
  public Database() throws SQLException {
    new File("db").mkdir();
    Connection connection;
    try {
      connection = DriverManager.getConnection(DB_URL, "sa", "");
    } catch (SQLException ex) {
      if (isLegacyFormat(ex)) {
        log.warn("Detected legacy H2 database; attempting automatic migration...");
        DatabaseMigrator migrator = new DatabaseMigrator(DB_BASE);
        migrator.migrateLegacyToCurrent();
        log.info("Migration complete. Reconnecting using upgraded database.");
        connection = DriverManager.getConnection(DB_URL, "sa", "");
      } else {
        throw ex;
      }
    }
    this.conn = connection;
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

  public void createIndexIfNotExists(
    String table, String column) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
      String.format("CREATE INDEX IF NOT EXISTS %s_%s_idx ON %s(%s);",
        table, column, table, column))) {
      ps.executeUpdate();
    }
  }

  private static boolean isLegacyFormat(SQLException ex) {
    String message = ex.getMessage();
    return message != null
      && (message.contains("Unsupported database file version")
        || message.contains("read format 1 is smaller"));
  }

}
