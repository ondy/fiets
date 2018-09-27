package fiets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fiets.db.Database;
import fiets.display.DefaultPostDisplayProvider;
import fiets.display.PostDisplayProvider;
import fiets.filter.DefaultRawPostFilter;
import fiets.filter.RawPostFilter;
import fiets.model.Post;
import fiets.views.View;
import jodd.json.JsonObject;

public class Server extends NanoHTTPD {

  private static final Logger log = LogManager.getLogger();
  private final Timer timer = new Timer("fiets-timer", true);
  private Database db;
  private FeedService fs;
  private PostDisplayProvider pdp;
  private RawPostFilter filter;

  public Server(int port,
    PostDisplayProvider thePdp, RawPostFilter theFilter)
    throws IOException {
    super(port);
    pdp = thePdp;
    filter = theFilter;
  }

  public static void main(String[] args) throws Exception {
    int port = 7000;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }
    initFietsProperties();
    PostDisplayProvider pdp = initPostDisplayProvider();
    RawPostFilter filter = initRawPostFilter();
    Server srv = new Server(port, pdp, filter);
    srv.init();
  }

  private static RawPostFilter initRawPostFilter()
    throws Exception {
    String filter = System.getProperty("rawpostfilter");
    if (filter == null) {
      return new DefaultRawPostFilter();
    } else {
      return (RawPostFilter) Class.forName(filter).newInstance();
    }
  }

  private static PostDisplayProvider initPostDisplayProvider() 
    throws Exception {
    String provider = System.getProperty("postdisplayprovider");
    if (provider == null) {
      return new DefaultPostDisplayProvider();
    } else {
      return (PostDisplayProvider) Class.forName(provider).newInstance();
    }
  }

  private static void initFietsProperties() throws IOException {
    File f = new File("fiets.properties");
    if (f.exists()) {
      Properties fprops = new Properties();
      fprops.load(new FileInputStream(f));
      for (String name : fprops.stringPropertyNames()) {
        System.setProperty(name, fprops.getProperty(name));
      }
    }
  }

  private void scheduleNowAndEvery(Runnable r, long everyMillis) {
    final TimerTask task = new TimerTask() {
      @Override public void run() {
        try {
          r.run();
        } catch (Throwable t) {
          log.error(
            "Could not complete scheduled task: {}", t.getMessage(), t);
        }
      }
    };
    timer.schedule(task, 0, everyMillis);
  }

  private void init() throws IOException, SQLException {
    start();
    try (final Database theDb = new Database()) {
      db = theDb;
      fs = new FeedService(db, filter);
      scheduleNowAndEvery(() -> {
        log.info("Triggering regular post update.");
        fs.updateAllPosts();
      }, minutesMillis(10));
      scheduleNowAndEvery(() -> {
        log.info("Deleting {} outdated posts.", fs.getOutdatedCount());
        fs.dropOutdated();
        log.info("Done deleting outdated posts.");
      }, dayMillis());
      waitForever();
    } finally {
      db = null;
    }
  }

  private static int minutesMillis(int minutes) {
    return 1000*60*minutes;
  }

  private static int dayMillis() {
    return minutesMillis(24*60);
  }

  private static void waitForever() {
    do {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } while (true);
  }

  @Override public Response serve(IHTTPSession session) {
    try {
      SessionDecorator sd = new SessionDecorator(session);
      PathMatch pm = PathMatch.match(sd);
      View<?> view = pm.serve(sd, fs, pdp);
      Object content = view.getContent();
      Response rsp;
      if (content instanceof String) {
        rsp = newFixedLengthResponse((String) content);
        rsp.setMimeType(view.getMimeType());
      } else if (content instanceof InputStream) {
        rsp = newChunkedResponse(
          Status.OK, view.getMimeType(), (InputStream) content);
      } else if (content instanceof PathMatch) {
        rsp = redirect(((PathMatch) content).getUrl());
      } else {
        throw new IllegalStateException(
          "Unknown content type: " + content.getClass());
      }
      return rsp;
    } catch (FileNotFoundException e) {
      return error(Status.NOT_FOUND, "File does not exist.", e);
    } catch (IllegalArgumentException e) {
      return error(Status.BAD_REQUEST, "Bad request: " + e, e);
    } catch (Exception e) {
      return error(Status.INTERNAL_ERROR, "Unexpected issue.", e);
    }
  }

  public static JsonObject jsonOk() {
    return new JsonObject().put("status", "OK");
  }

  public static Response error(Status status, String msg, Throwable t) {
    if (t == null) {
      log.error(msg);
    } else {
      msg = String.format("%s (%s)", msg, t.getMessage());
      log.error(msg, t);
    }
    return newFixedLengthResponse(status, MIME_HTML, msg);
  }

  @SuppressWarnings("deprecation")
  private Response redirect(String target) {
    Response rsp = newFixedLengthResponse(
      Status.FOUND, "text/plain", "Redirecting to " + target);
    rsp.addHeader("Location", target);
    return rsp;
  }

  public static Set<Long> getIds(Collection<Post> posts) {
    Set<Long> ids = new HashSet<>();
    for (Post p : posts) {
      ids.add(p.getId());
    }
    return ids;
  }
}
