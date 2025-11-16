package fiets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DeploymentInfo {
  private static final Logger log = LogManager.getLogger();
  private static final String UNKNOWN = "unbekannt";
  private static final String BRANCH = determineBranch();
  private static final String COMMIT_TIME = determineCommitTime();

  private DeploymentInfo() {}

  public static String getDisplayText() {
    String branch = defaultIfBlank(BRANCH, UNKNOWN);
    String commitTime = defaultIfBlank(COMMIT_TIME, UNKNOWN);
    return String.format("Branch: %s • Letzter Commit: %s",
      escape(branch), escape(commitTime));
  }

  private static String determineBranch() {
    String value = System.getProperty("fiets.branch");
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
    return runGitCommand("log", "-1", "--format=%cd",
      "--date=format:%Y-%m-%d %H:%M:%S %z");
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

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;");
  }
}
