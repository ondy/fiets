package fiets.model;

import java.util.Locale;

public final class FeedMetadata {

  private static final String COLOR_PREFIX = "|fc=";
  private static final String DEFAULT_COLOR = "#e8eef7";

  private FeedMetadata() {}

  public static String extractColor(Feed feed) {
    if (feed == null) {
      return null;
    }
    return extractColor(feed.getLastStatus());
  }

  public static String extractColor(String status) {
    if (status == null) {
      return null;
    }
    int pos = status.indexOf(COLOR_PREFIX);
    if (pos < 0 || pos + COLOR_PREFIX.length() >= status.length()) {
      return null;
    }
    return normalizeColor(status.substring(pos + COLOR_PREFIX.length()));
  }

  public static String mergeStatusAndColor(String status, String existingStatus,
      String color) {
    String baseStatus = stripColor(status != null ? status : existingStatus);
    String persistedColor = normalizeColor(color != null
      ? color : extractColor(existingStatus));
    if (persistedColor == null) {
      return baseStatus;
    }
    return baseStatus + COLOR_PREFIX + persistedColor;
  }

  public static String stripColor(String status) {
    if (status == null) {
      return "unknown";
    }
    int pos = status.indexOf(COLOR_PREFIX);
    if (pos < 0) {
      return status;
    }
    return status.substring(0, pos);
  }

  public static String normalizeColor(String color) {
    if (color == null) {
      return null;
    }
    String trimmed = color.trim();
    if (trimmed.length() == 0) {
      return null;
    }
    if (!trimmed.startsWith("#")) {
      trimmed = "#" + trimmed;
    }
    if (trimmed.length() != 7) {
      return null;
    }
    try {
      Integer.parseInt(trimmed.substring(1), 16);
      return trimmed.toLowerCase(Locale.ROOT);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static String defaultColor() {
    return DEFAULT_COLOR;
  }

  public static String ensureColorOrDefault(String color) {
    String normalized = normalizeColor(color);
    return normalized == null ? DEFAULT_COLOR : normalized;
  }

  public static String readableTextColor(String backgroundColor) {
    String normalized = normalizeColor(backgroundColor);
    if (normalized == null) {
      normalized = DEFAULT_COLOR;
    }
    int r = Integer.parseInt(normalized.substring(1, 3), 16);
    int g = Integer.parseInt(normalized.substring(3, 5), 16);
    int b = Integer.parseInt(normalized.substring(5, 7), 16);
    double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.6 ? "#111827" : "#f8fafc";
  }
}
