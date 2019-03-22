package fiets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.ResponseException;

public class SessionDecorator {

  private final IHTTPSession session;
  private final String path;
  private final String mainPath;

  public SessionDecorator(IHTTPSession theSession) {
    session = theSession;
    path = extractPath(session);
    mainPath = extractMainPath(path);
  }

  private static String extractMainPath(String thePath) {
    String mainPath = thePath;
    int slashPos = mainPath.indexOf('/');
    if (slashPos > 0) {
      mainPath = thePath.substring(0, slashPos);
    }
    return mainPath;
  }

  private static String extractPath(IHTTPSession theSession) {
    String path = theSession.getUri();
    path = path.replace("/..", "");
    if (path.endsWith("/")) {
      path = path.substring(0, path.length()-1);
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return path;
  }

  public String getMainPath() {
    return mainPath;
  }

  public String getPath() {
    return path;
  }

  public int intParam(String name) {
    List<String> values = session.getParameters().get(name);
    if (values == null || values.size() != 1) {
      throw new IllegalArgumentException(String.format(
        "Wrong value provided for '%s' parameter: %s", name, values));
    }
    return Integer.parseInt(values.get(0));
  }

  public int intParamOr(String name, int defaultValue) {
    List<String> values = session.getParameters().get(name);
    if (values == null || values.size() != 1) {
      return defaultValue;
    }
    return Integer.parseInt(values.get(0));
  }

  public List<Long> longParams(String name) {
    List<String> values = session.getParameters().get(name);
    List<Long> longs = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        String[] parts = value.split(",");
        for (String part : parts) {
          longs.add(Long.valueOf(part));
        }
      }
    }
    return longs;
  }

  public long longParam(String name) {
    List<String> values = session.getParameters().get(name);
    if (values == null || values.size() != 1) {
      throw new IllegalArgumentException(String.format(
        "Wrong value provided for '%s' parameter: %s", name, values));
    }
    return Long.parseLong(values.get(0));
  }

  public String stringParam(String name) {
    List<String> stringParams = stringParams(name);
    return stringParams != null && stringParams.size() > 0 
      ? stringParams.get(0) : null;
  }

  public List<String> stringParams(String name) {
    return session.getParameters().get(name);
  }

  public Map<String, List<String>> getParameters() {
    return session.getParameters();
  }

  public Map<String, List<String>> postParameters() 
    throws IOException, ResponseException {
    session.parseBody(new HashMap<>());
    return session.getParameters();
  }

}
