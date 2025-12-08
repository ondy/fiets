package fiets;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.db.FeedDao;
import fiets.db.PostDao;
import fiets.model.Feed;
import fiets.model.FeedMetadata;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public class FeedColorService {

  private static final Logger log = LogManager.getLogger();
  private final FeedDao feedDao;
  private final PostDao postDao;

  public FeedColorService(FeedDao theFeedDao, PostDao thePostDao) {
    feedDao = theFeedDao;
    postDao = thePostDao;
  }

  public String ensureColor(Feed feed) {
    String color = FeedMetadata.extractColor(feed);
    if (color != null) {
      return color;
    }
    color = FeedMetadata.ensureColorOrDefault(distinctColor(determineColor(feed)));
    try {
      feedDao.storeFeedColor(feed, color);
    } catch (SQLException e) {
      log.warn("Could not persist feed color for {}: {}", feed, e.getMessage());
    }
    return color;
  }

  private String determineColor(Feed feed) {
    URI baseUri = baseUri(feed.getLocation());
    if (baseUri == null) {
      return null;
    }
    String faviconColor = readFaviconColor(baseUri);
    if (faviconColor != null) {
      return faviconColor;
    }
    URI postUri = firstPostBaseUri(feed.getId());
    if (postUri != null) {
      String postFaviconColor = readFaviconColor(postUri);
      if (postFaviconColor != null) {
        return postFaviconColor;
      }
    }
    return null;
  }

  private static URI baseUri(String location) {
    try {
      URI uri = new URI(location);
      if (uri.getScheme() == null || uri.getHost() == null) {
        return null;
      }
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
        uri.getPort(), "/", null, null);
    } catch (URISyntaxException e) {
      log.debug("Could not parse feed location {}", location, e);
      return null;
    }
  }

  private String readFaviconColor(URI baseUri) {
    try {
      URI faviconUri = baseUri.resolve("/favicon.ico");
      HttpResponse rsp = HttpRequest.get(faviconUri.toString())
        .timeout(5000)
        .connectionTimeout(5000)
        .followRedirects(true)
        .trustAllCerts(true)
        .send();
      if (rsp.statusCode() >= 400) {
        return null;
      }
      byte[] bytes = rsp.bodyBytes();
      if (bytes == null || bytes.length == 0) {
        return null;
      }
      return averageColor(bytes);
    } catch (RuntimeException e) {
      log.debug("Could not read favicon for {}: {}", baseUri, e.toString());
      return null;
    }
  }

  private String averageColor(byte[] bytes) {
    try {
      BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
      if (img == null) {
        img = readPngFromIco(bytes);
      }
      if (img == null) {
        return null;
      }
      return dominantColor(img);
    } catch (Exception e) {
      log.debug("Could not determine favicon color: {}", e.toString());
      return null;
    }
  }

  private BufferedImage readPngFromIco(byte[] bytes) {
    if (bytes.length < 6) {
      return null;
    }
    int count = Byte.toUnsignedInt(bytes[4]) | (Byte.toUnsignedInt(bytes[5]) << 8);
    int headerSize = 6 + (count * 16);
    if (headerSize > bytes.length) {
      return null;
    }
    BufferedImage best = null;
    for (int i = 0; i < count; i++) {
      int entryOffset = 6 + (i * 16);
      int imageSize = readInt(bytes, entryOffset + 8);
      int imageOffset = readInt(bytes, entryOffset + 12);
      if (imageOffset < 0 || imageSize <= 0 || imageOffset + imageSize > bytes.length) {
        continue;
      }
      if (!isPng(bytes, imageOffset)) {
        continue;
      }
      try {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes, imageOffset, imageSize));
        if (img != null && (best == null || (img.getWidth() * img.getHeight()) > (best.getWidth() * best.getHeight()))) {
          best = img;
        }
      } catch (Exception e) {
        log.debug("Could not read PNG frame from ICO: {}", e.toString());
      }
    }
    return best;
  }

  private static int readInt(byte[] bytes, int offset) {
    return Byte.toUnsignedInt(bytes[offset])
      | (Byte.toUnsignedInt(bytes[offset + 1]) << 8)
      | (Byte.toUnsignedInt(bytes[offset + 2]) << 16)
      | (Byte.toUnsignedInt(bytes[offset + 3]) << 24);
  }

  private static boolean isPng(byte[] bytes, int offset) {
    if (offset + 8 > bytes.length) {
      return false;
    }
    return bytes[offset] == (byte) 0x89
      && bytes[offset + 1] == 0x50
      && bytes[offset + 2] == 0x4e
      && bytes[offset + 3] == 0x47
      && bytes[offset + 4] == 0x0d
      && bytes[offset + 5] == 0x0a
      && bytes[offset + 6] == 0x1a
      && bytes[offset + 7] == 0x0a;
  }

  private String dominantColor(BufferedImage img) {
    Map<Integer, Integer> histogram = new HashMap<>();
    int width = img.getWidth();
    int height = img.getHeight();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int argb = img.getRGB(x, y);
        int alpha = (argb >> 24) & 0xff;
        if (alpha == 0) {
          continue;
        }
        histogram.merge(argb & 0x00ffffff, 1, Integer::sum);
      }
    }
    if (histogram.isEmpty()) {
      return null;
    }
    int dominant = histogram.entrySet().stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .orElse(0);
    Color color = new Color(dominant);
    return FeedMetadata.normalizeColor(String.format("#%02x%02x%02x",
      color.getRed(), color.getGreen(), color.getBlue()));
  }

  private String distinctColor(String proposed) {
    String normalized = FeedMetadata.normalizeColor(proposed);
    if (normalized == null) {
      return proposed;
    }
    List<Double> existingHues = existingHues();
    if (existingHues.isEmpty()) {
      return normalized;
    }
    float[] hsb = Color.RGBtoHSB(
      Integer.parseInt(normalized.substring(1, 3), 16),
      Integer.parseInt(normalized.substring(3, 5), 16),
      Integer.parseInt(normalized.substring(5, 7), 16),
      null);
    double hue = hsb[0] * 360.0d;
    double minDistance = minHueDistance(hue, existingHues);
    if (minDistance >= 18d) {
      return normalized;
    }
    double bestHue = hue;
    double widestGap = -1d;
    List<Double> sorted = new ArrayList<>(existingHues);
    sorted.sort(Double::compareTo);
    for (int i = 0; i < sorted.size(); i++) {
      double start = sorted.get(i);
      double end = sorted.get((i + 1) % sorted.size());
      double gap = end - start;
      if (gap < 0) {
        gap += 360d;
      }
      if (gap > widestGap) {
        widestGap = gap;
        bestHue = start + gap / 2d;
      }
    }
    while (bestHue >= 360d) {
      bestHue -= 360d;
    }
    int rgb = Color.HSBtoRGB((float) (bestHue / 360d), hsb[1], hsb[2]);
    Color color = new Color(rgb);
    return FeedMetadata.normalizeColor(String.format("#%02x%02x%02x",
      color.getRed(), color.getGreen(), color.getBlue()));
  }

  private List<Double> existingHues() {
    try {
      List<String> colors = feedDao.getAllFeedColors();
      List<Double> hues = new ArrayList<>();
      for (String color : colors) {
        if (color == null) {
          continue;
        }
        float[] hsb = Color.RGBtoHSB(
          Integer.parseInt(color.substring(1, 3), 16),
          Integer.parseInt(color.substring(3, 5), 16),
          Integer.parseInt(color.substring(5, 7), 16),
          null);
        hues.add(hsb[0] * 360.0d);
      }
      return hues;
    } catch (SQLException e) {
      log.debug("Could not load stored feed colors: {}", e.toString());
      return List.of();
    }
  }

  private static double minHueDistance(double hue, List<Double> existingHues) {
    double minDistance = 360d;
    for (double existing : existingHues) {
      double direct = Math.abs(hue - existing);
      double wrapped = 360d - direct;
      minDistance = Math.min(minDistance, Math.min(direct, wrapped));
    }
    return minDistance;
  }

  private URI firstPostBaseUri(long feedId) {
    try {
      String postLocation = postDao.getMostRecentPostLocation(feedId);
      if (postLocation == null) {
        return null;
      }
      return baseUri(postLocation);
    } catch (SQLException e) {
      log.debug("Could not load most recent post for feed {}: {}", feedId, e.toString());
      return null;
    }
  }
}
