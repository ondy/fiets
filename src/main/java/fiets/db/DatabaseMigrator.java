package fiets.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.RunScript;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles automatic migration of legacy H2 1.4 databases to the current
 * H2 2.x format without requiring a manual script run.
 */
public class DatabaseMigrator {

  private static final Logger log = LogManager.getLogger();
  private static final String LEGACY_JAR_URL =
    "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar";
  private static final String LEGACY_JAR_NAME = "h2-1.4.200.jar";
  private static final Pattern SEQUENCE_PATTERN = Pattern.compile(
    "CREATE SEQUENCE\\s+\\\"?(?:PUBLIC\\\"\\.)?\\\"?([A-Z0-9_]+)\\\"?.*START WITH\\s+([0-9]+)",
    Pattern.CASE_INSENSITIVE);

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
      Path sanitizedExport = sanitizeExport(exportScript);
      try {
        backupLegacyFiles(dbBackup, traceBackup);
        backupCreated = true;
      } catch (AccessDeniedException ade) {
        targetDbBase = dbBase + "-v2";
        log.warn("Could not move legacy database for backup ({}). "
            + "Leaving the original file untouched and importing into {} instead.",
          ade.getMessage(), targetDbBase);
      }
      try {
        importWithCurrentEngine(sanitizedExport, targetDbBase);
      } catch (SQLException | IOException e) {
        if (isAccessDenied(e)) {
          targetDbBase = migrateToTempLocation(sanitizedExport, targetDbBase);
        } else {
          throw e;
        }
      }

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

  private String migrateToTempLocation(Path exportScript, String failedTarget)
    throws SQLException, IOException {
    Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "fiets-db");
    Files.createDirectories(tmpDir);
    String tempBase = tmpDir.resolve("fiets-v2").toString();
    log.warn(
      "Cannot write migrated H2 database to {} due to permissions. "
        + "Re-trying import in writable temp directory at {}.",
      failedTarget, tempBase);
    importWithCurrentEngine(exportScript, tempBase);
    log.info("Migration succeeded using temp directory fallback at {}.mv.db", tempBase);
    return tempBase;
  }

  private String buildCurrentUrl(String targetDbBase) {
    return "jdbc:h2:" + targetDbBase
      + ";MODE=LEGACY;DATABASE_TO_LOWER=TRUE;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0";
  }

  private boolean isAccessDenied(Exception e) {
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof AccessDeniedException) {
        return true;
      }
      cause = cause.getCause();
    }
    String message = e.getMessage();
    return message != null && message.toLowerCase().contains("access denied");
  }

  private Path sanitizeExport(Path exportScript) throws IOException {
    Path sanitized = exportScript.getParent().resolve("export-sanitized.sql");

    // First pass: capture sequence start values so we can rewrite identity columns.
    Map<String, Long> sequenceStarts = new HashMap<>();
    try (BufferedReader reader = Files.newBufferedReader(exportScript, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = SEQUENCE_PATTERN.matcher(line);
        if (matcher.find()) {
          String seqName = matcher.group(1);
          long start = Long.parseLong(matcher.group(2));
          sequenceStarts.put(seqName, start);
        }
      }
    }

    try (BufferedReader reader = Files.newBufferedReader(exportScript, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(sanitized, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim().toUpperCase();
        if (trimmed.startsWith("CREATE SEQUENCE")
          || trimmed.startsWith("DROP SEQUENCE")
          || trimmed.startsWith("ALTER SEQUENCE")) {
          continue;
        }

        String normalized = line.replace("\"PUBLIC\".", "")
          .replace("PUBLIC.", "");

        for (Map.Entry<String, Long> entry : sequenceStarts.entrySet()) {
          String quoted = "\"" + entry.getKey() + "\"";
          if (normalized.toUpperCase().contains("DEFAULT NEXT VALUE FOR " + quoted)) {
            normalized = normalized.replace("DEFAULT NEXT VALUE FOR " + quoted,
              "GENERATED BY DEFAULT AS IDENTITY (START WITH " + entry.getValue() + ")");
          }
          normalized = normalized.replace("SEQUENCE " + quoted, "")
            .replace("NULL_TO_DEFAULT", "");
        }

        if (normalized.toUpperCase().contains("DEFAULT NEXT VALUE FOR")) {
          normalized = normalized.replaceFirst(
            "DEFAULT NEXT VALUE FOR [\\\"A-Z0-9_]+",
            "GENERATED BY DEFAULT AS IDENTITY (START WITH 1)");
        }

        normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ");
        normalized = normalized.replaceAll("(?i)\\bSELECTIVITY\\s+\\d+\\b", "");
        normalized = normalized.replaceAll("(?i)\\bSELECTIVITY\\b", "");
        normalized = normalized.replaceAll("\\s{2,}", " ").trim();

        writer.write(normalized);
        writer.newLine();
      }
    }
    return sanitized;
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
    clearExistingTarget(targetDbBase);
    String url = buildCurrentUrl(targetDbBase);
    try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
      ensurePublicSchema(conn);
      try (InputStreamReader reader = new InputStreamReader(
        Files.newInputStream(exportScript), StandardCharsets.UTF_8)) {
        RunScript.execute(conn, reader);
      }
      resetIdentitySequences(conn);
    }
  }

  private void clearExistingTarget(String targetDbBase) throws IOException {
    Path dbFile = Paths.get(targetDbBase + ".mv.db");
    Path traceFile = Paths.get(targetDbBase + ".trace.db");
    Files.deleteIfExists(dbFile);
    Files.deleteIfExists(traceFile);
  }

  private void ensurePublicSchema(Connection conn) throws SQLException {
    try (PreparedStatement create = conn.prepareStatement(
      "CREATE SCHEMA IF NOT EXISTS \"PUBLIC\"")) {
      create.execute();
    }
    try (PreparedStatement set = conn.prepareStatement("SET SCHEMA \"PUBLIC\"")) {
      set.execute();
    }
  }

  private void resetIdentitySequences(Connection conn) throws SQLException {
    resetIdentitySequence(conn, "feed");
    resetIdentitySequence(conn, "filter");
    resetIdentitySequence(conn, "post");
  }

  private void resetIdentitySequence(Connection conn, String table) throws SQLException {
    long maxId;
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT COALESCE(MAX(id), 0) FROM " + table)) {
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        maxId = rs.getLong(1);
      }
    }
    long nextId = maxId + 1;
    try (PreparedStatement ps = conn.prepareStatement(
      "ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + nextId)) {
      ps.execute();
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
