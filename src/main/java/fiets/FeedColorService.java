package fiets;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.db.FeedDao;
import fiets.model.Feed;
import fiets.model.FeedMetadata;
import fiets.sources.HttpFeedSource;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public class FeedColorService {

  private static final Logger log = LogManager.getLogger();
  private final FeedDao feedDao;

  public FeedColorService(FeedDao theFeedDao) {
    feedDao = theFeedDao;
  }

  public String ensureColor(Feed feed) {
    String color = FeedMetadata.extractColor(feed);
    if (color != null) {
      return color;
    }
    color = FeedMetadata.ensureColorOrDefault(determineColor(feed));
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
    String homepageColor = readHomepageColor(baseUri);
    if (homepageColor != null) {
      return homepageColor;
    }
    return hashToColor(baseUri.toString());
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
        return null;
      }
      long r = 0;
      long g = 0;
      long b = 0;
      long count = 0;
      for (int x = 0; x < img.getWidth(); x++) {
        for (int y = 0; y < img.getHeight(); y++) {
          int argb = img.getRGB(x, y);
          int alpha = (argb >> 24) & 0xff;
          if (alpha == 0) {
            continue;
          }
          Color c = new Color(argb, true);
          r += c.getRed();
          g += c.getGreen();
          b += c.getBlue();
          count++;
        }
      }
      if (count == 0) {
        return null;
      }
      return FeedMetadata.normalizeColor(String.format("#%02x%02x%02x",
        r / count, g / count, b / count));
    } catch (Exception e) {
      log.debug("Could not average favicon color: {}", e.toString());
      return null;
    }
  }

  private String readHomepageColor(URI baseUri) {
    try {
      String html = HttpFeedSource.readUrlContent(baseUri.toString());
      return hashToColor(html);
    } catch (RuntimeException e) {
      log.debug("Could not read homepage for {}: {}", baseUri, e.toString());
      return null;
    }
  }

  private String hashToColor(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      int hue = ((hash[0] & 0xff) << 8 | (hash[1] & 0xff)) % 360;
      float saturation = 0.45f + ((hash[2] & 0xff) / 255f) * 0.25f;
      float lightness = 0.78f;
      return FeedMetadata.normalizeColor(hslToHex(hue, saturation, lightness));
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  private static String hslToHex(float h, float s, float l) {
    float c = (1 - Math.abs(2 * l - 1)) * s;
    float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
    float m = l - c / 2;
    float rPrime = 0, gPrime = 0, bPrime = 0;
    if (h < 60) {
      rPrime = c; gPrime = x; bPrime = 0;
    } else if (h < 120) {
      rPrime = x; gPrime = c; bPrime = 0;
    } else if (h < 180) {
      rPrime = 0; gPrime = c; bPrime = x;
    } else if (h < 240) {
      rPrime = 0; gPrime = x; bPrime = c;
    } else if (h < 300) {
      rPrime = x; gPrime = 0; bPrime = c;
    } else {
      rPrime = c; gPrime = 0; bPrime = x;
    }
    int r = Math.round((rPrime + m) * 255);
    int g = Math.round((gPrime + m) * 255);
    int b = Math.round((bPrime + m) * 255);
    return String.format("#%02x%02x%02x", r, g, b);
  }
}
