package fiets.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import java.nio.file.AccessDeniedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles automatic migration of legacy H2 1.4 databases to the current
 * H2 2.x format without requiring a manual script run.
 */
public class DatabaseMigrator {

  private static final Logger log = LogManager.getLogger();
  private static final String LEGACY_JAR_URL =
    "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar";
  private static final String LEGACY_JAR_NAME = "h2-1.4.200.jar";

  private final String dbBase;

  public DatabaseMigrator(String dbBase) {
    this.dbBase = dbBase;
  }

  public String migrateLegacyToCurrent() throws SQLException {
    Path dbFile = Paths.get(dbBase + ".mv.db");
    if (!Files.exists(dbFile)) {
      throw new SQLException("Legacy H2 database not found at " + dbFile);
    }

    Path dbBackup = Paths.get(dbBase + ".mv.db.legacy");
    Path traceBackup = Paths.get(dbBase + ".trace.db.legacy");
    boolean backupCreated = false;
    String targetDbBase = dbBase;

    Path toolsDir = Paths.get("build", "h2");
    Path legacyJar = toolsDir.resolve(LEGACY_JAR_NAME);
    Path exportScript = toolsDir.resolve("export.sql");
    try {
      Files.createDirectories(toolsDir);
      downloadIfMissing(legacyJar);
      exportWithLegacyEngine(legacyJar, exportScript);
      try {
        backupLegacyFiles(dbBackup, traceBackup);
        backupCreated = true;
      } catch (AccessDeniedException ade) {
        targetDbBase = dbBase + "-v2";
        log.warn("Could not move legacy database for backup ({}). "
            + "Leaving the original file untouched and importing into {} instead.",
          ade.getMessage(), targetDbBase);
      }
      importWithCurrentEngine(exportScript, targetDbBase);
      log.info("Automatic H2 migration completed. New database at {}.mv.db", targetDbBase);
      return targetDbBase;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new SQLException("Failed to migrate H2 database automatically", e);
    } catch (SQLException e) {
      if (backupCreated) {
        restoreBackup(dbFile, dbBackup, traceBackup);
      }
      throw e;
    }
  }

  private void downloadIfMissing(Path legacyJar) throws IOException {
    if (Files.exists(legacyJar)) {
      return;
    }
    log.info("Downloading legacy H2 helper to {}", legacyJar);
    HttpURLConnection conn = (HttpURLConnection) new URL(LEGACY_JAR_URL).openConnection();
    conn.setRequestProperty("User-Agent", "fiets-migrator");
    conn.setInstanceFollowRedirects(true);
    conn.connect();
    int status = conn.getResponseCode();
    if (status != HttpURLConnection.HTTP_OK) {
      throw new IOException("Failed to download legacy H2 jar: HTTP " + status);
    }
    try (InputStream in = conn.getInputStream()) {
      Files.copy(in, legacyJar);
    }
  }

  private void exportWithLegacyEngine(Path legacyJar, Path exportScript)
    throws IOException, InterruptedException, SQLException {
    ProcessBuilder pb = new ProcessBuilder(
      "java", "-cp", legacyJar.toString(),
      "org.h2.tools.Script",
      "-url", "jdbc:h2:file:" + dbBase,
      "-user", "sa",
      "-password", "",
      "-script", exportScript.toString());
    pb.redirectErrorStream(true);
    Process p = pb.start();
    String output = readProcessOutput(p);
    int exit = p.waitFor();
    if (exit != 0) {
      throw new SQLException("Legacy export failed (exit " + exit + "): " + output);
    }
  }

  private void backupLegacyFiles(Path dbBackup, Path traceBackup) throws IOException {
    Path dbFile = Paths.get(dbBase + ".mv.db");
    Path traceFile = Paths.get(dbBase + ".trace.db");

    Files.move(dbFile, dbBackup, StandardCopyOption.REPLACE_EXISTING);
    if (Files.exists(traceFile)) {
      Files.move(traceFile, traceBackup, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void restoreBackup(Path dbFile, Path dbBackup, Path traceBackup) {
    try {
      if (Files.exists(dbBackup)) {
        Files.move(dbBackup, dbFile);
      }
      Path traceFile = Paths.get(dbBase + ".trace.db");
      if (Files.exists(traceBackup)) {
        Files.move(traceBackup, traceFile);
      }
    } catch (IOException restoreError) {
      log.error("Failed to restore legacy database after migration error: {}",
        restoreError.getMessage(), restoreError);
    }
  }

  private void importWithCurrentEngine(Path exportScript, String targetDbBase)
    throws SQLException, IOException {
    String url = "jdbc:h2:" + targetDbBase + ";MODE=LEGACY;DATABASE_TO_LOWER=TRUE";
    try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
      try (InputStreamReader reader = new InputStreamReader(
        Files.newInputStream(exportScript), StandardCharsets.UTF_8)) {
        RunScript.execute(conn, reader);
      }
    }
  }

  private String readProcessOutput(Process p) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStream in = p.getInputStream()) {
      byte[] buf = new byte[4096];
      int read;
      while ((read = in.read(buf)) >= 0) {
        sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
      }
    }
    return sb.toString().trim();
  }
}
