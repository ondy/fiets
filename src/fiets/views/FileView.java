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
      stream = Server.class.getResourceAsStream("/META-INF/resources/" + path);
    }
    if (stream == null) {
      throw new FileNotFoundException(path);
    }
  }

  private String determineMimeType(String path) {
    if (path.endsWith(".css")) {
      return "text/css";
    } else if (path.endsWith(".js")) {
      return "application/javascript";
    } else if (path.endsWith(".map")) {
      return "application/json";
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
