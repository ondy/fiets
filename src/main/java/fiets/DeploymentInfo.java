package fiets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DeploymentInfo {
  private static final Logger log = LogManager.getLogger();
  private static final String UNKNOWN = "unbekannt";
  private static final String PROPERTIES_PATH = "/fiets/deployment-info.properties";
  private static final Properties BUILD_PROPERTIES = loadBuildProperties();
  private static final String BRANCH = determineBranch();
  private static final String COMMIT_TIME = determineCommitTime();
  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");
  private static final DateTimeFormatter COMMIT_TIME_INPUT_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
  private static final DateTimeFormatter COMMIT_TIME_OUTPUT_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.GERMAN);

  private DeploymentInfo() {}

  public static String getDisplayText() {
    String branch = defaultIfBlank(BRANCH, UNKNOWN);
    String commitTime = formatCommitTime(defaultIfBlank(COMMIT_TIME, UNKNOWN));
    return String.format("%s (%s)", escape(commitTime), escape(branch));
  }

  private static String determineBranch() {
    String value = System.getProperty("fiets.branch");
    if (!isBlank(value)) {
      return value.trim();
    }
    value = getBuildProperty("branch");
    if (!isBlank(value)) {
      return value.trim();
    }
    return runGitCommand("rev-parse", "--abbrev-ref", "HEAD");
  }

  private static String determineCommitTime() {
    String value = System.getProperty("fiets.commitTime");
    if (!isBlank(value)) {
      return value.trim();
    }
    value = getBuildProperty("commitTime");
    if (!isBlank(value)) {
      return value.trim();
    }
    return runGitCommand("log", "-1", "--format=%cd",
      "--date=format:%Y-%m-%d %H:%M:%S %z");
  }

  private static String getBuildProperty(String key) {
    return BUILD_PROPERTIES.getProperty(key);
  }

  private static String runGitCommand(String... args) {
    List<String> command = new ArrayList<>(args.length + 1);
    command.add("git");
    for (String arg : args) {
      command.add(arg);
    }
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = readProcessOutput(process);
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        return output.trim();
      }
      log.warn("Git command {} exited with code {}", command, exitCode);
    } catch (IOException e) {
      log.warn("Could not execute git command {}", command, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while executing git command {}", command, e);
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
    return null;
  }

  private static String readProcessOutput(Process process) throws IOException {
    StringBuilder builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
      process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append('\n');
      }
    }
    return builder.toString();
  }

  private static String defaultIfBlank(String value, String defaultValue) {
    return isBlank(value) ? defaultValue : value.trim();
  }

  private static Properties loadBuildProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream = DeploymentInfo.class
        .getResourceAsStream(PROPERTIES_PATH)) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (IOException e) {
      log.warn("Could not read deployment info properties", e);
    }
    return properties;
  }

  private static String formatCommitTime(String commitTime) {
    if (isBlank(commitTime) || UNKNOWN.equals(commitTime)) {
      return UNKNOWN;
    }
    String trimmed = commitTime.trim();
    try {
      OffsetDateTime parsed = OffsetDateTime.parse(trimmed, COMMIT_TIME_INPUT_FORMAT);
      return parsed.atZoneSameInstant(DISPLAY_ZONE).format(COMMIT_TIME_OUTPUT_FORMAT);
    } catch (DateTimeParseException e) {
      log.debug("Could not parse commit time '{}'", trimmed, e);
      return trimmed;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;");
  }
}
