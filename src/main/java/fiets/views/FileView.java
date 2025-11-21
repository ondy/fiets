package fiets.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import fiets.Server;
import fiets.SessionDecorator;

public class FileView implements View<InputStream> {

  private final InputStream stream;
  private final String mimeType;

  public FileView(SessionDecorator sd) throws FileNotFoundException {
    String path = sd.getPath();
    mimeType = determineMimeType(path);
    File localFile = new File(path);
    if (localFile.exists()) {
      stream = new FileInputStream(localFile);
    } else {
      stream = loadFromClasspath(path);
    }
    if (stream == null) {
      throw new FileNotFoundException(path);
    }
  }

  private InputStream loadFromClasspath(String path) {
    InputStream classpathStream = Server.class.getResourceAsStream("/" + path);
    if (classpathStream != null) {
      return classpathStream;
    }
    return Server.class.getResourceAsStream("/META-INF/resources/" + path);
  }

  private String determineMimeType(String path) {
    if (path.endsWith(".css")) {
      return "text/css";
    } else if (path.endsWith(".js")) {
      return "application/javascript";
    } else if (path.endsWith(".map")) {
      return "application/json";
    } else if (path.endsWith(".png")) {
      return "image/png";
    } else if (path.endsWith(".ico")) {
      return "image/x-icon";
    } else if (path.endsWith(".webmanifest")) {
      return "application/manifest+json";
    } else {
      throw new IllegalArgumentException("Unknown filetype to serve: " + path);
    }
  }

  @Override public String getMimeType() {
    return mimeType;
  }

  @Override public InputStream getContent() {
    return stream;
  }

}
