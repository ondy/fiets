package fiets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.iki.elonen.NanoHTTPD.ResponseException;
import fiets.model.Feed;
import fiets.model.HasId;
import fiets.model.Post;
import fiets.views.JsonView;
import fiets.views.View;
import jodd.json.JsonArray;
import jodd.json.JsonObject;

public class FeverApi {

  private static final Logger log = LogManager.getLogger();
  private String key;

  public FeverApi() {
    String email = System.getProperty("feverapi.email");
    String pwd = System.getProperty("feverapi.password");
    if (email == null || pwd == null) {
      throw new IllegalArgumentException(
        "Both system properties feverapi.email and feverapi.password "
        + "have to be set to enable Fever API.");
    }
    key = md5(email + ':' + pwd);
  }

  public View<String> serve(SessionDecorator sd, FeedService fs)
    throws IOException, ResponseException, SQLException {
    Map<String, List<String>> getParams = sd.getParameters();
    log.info("GET params: {}", getParams);
    if (getParams.containsKey("refresh")) {
      return new JsonView(new JsonObject());
    } else if (getParams.containsKey("api")) {
      Map<String, List<String>> postParams = sd.postParameters();
      log.info("POST params: {}", postParams);
      String apiKey = sd.stringParam("api_key");
      boolean auth = key.equals(apiKey);
      JsonObject json = new JsonObject()
          .put("api_version", 3)
          .put("auth", auth ? 1 : 0);
      if (auth) {
        json.put("last_refreshed_on_time", fs.lastFeedUpdate());
        if (getParams.containsKey("feeds")) {
          List<Feed> feeds = fs.getAllFeeds();
          json
            .put("feeds", feedsArray(feeds));
        }
        if (getParams.containsKey("groups")) {
          log.info("Unsupported 'groups' command. ({})", getParams);
        }
        if (getParams.containsKey("unread_item_ids")) {
          json.put("unread_item_ids", unreadIds(fs));
        }
        if (getParams.containsKey("saved_item_ids")) {
          json.put("saved_item_ids", savedIds(fs));
        }
        if (getParams.containsKey("items")) {
          List<Post> posts;
          if (getParams.containsKey("since_id")) {
            long sinceId = sd.longParam("since_id");
            posts = fs.postsAfter(sinceId);
          } else if (getParams.containsKey("max_id")) {
            long maxId = sd.longParam("max_id");
            posts = fs.postsBefore(maxId);
          } else if (getParams.containsKey("with_ids")) {
            List<Long> withIds = sd.longParams("with_ids");
            posts = fs.posts(withIds);
          } else {
            String msg = String.format(
              "Unsupported items request: %s", getParams);
            log.error(msg);
            throw new IllegalArgumentException(msg);
          }
          Set<Long> bookmarks = fs.getBookmarks();
          json
            .put("items", itemsArray(posts, bookmarks))
            .put("total_items", fs.getFullCount());
        }
        if (postParams.containsKey("mark")) {
          mark(fs, sd.stringParam("mark"), sd.stringParam("as"),
            sd.stringParam("id"), sd.stringParam("before"));
        }
      }
      return new JsonView(json);
    } else {
      throw new FileNotFoundException(
        String.format("API query %s not supported.", getParams));
    }
  }

  private void mark(FeedService fs, String mark, String as, String idString,
    String beforeString)
    throws SQLException {
    long id = Long.parseLong(idString);
    if ("item".equals(mark)) {
      if ("read".equals(as)) {
        fs.markPostRead(id);
      } else if ("unread".equals(as)) {
        fs.markPostUnread(id);
      } else if ("saved".equals(as)) {
        fs.bookmarkPost(id);
      } else if ("unsaved".equals(as)) {
        fs.removeBookmarkPost(id);
      } else {
        log.error("Cannot mark '{}:{}' as '{}'.", mark, id, as);
      }
    } else if ("group".equals(mark) && id == 0l && "read".equals(as)) {
      long before = Long.parseLong(beforeString);
      fs.markAllRead(new Date(before*1000L));
    } else {
      log.error("Cannot mark '{}:{}' as '{}'.", mark, id, as);
    }
  }

  private String savedIds(FeedService fs) throws SQLException {
    List<Post> posts = fs.getBookmarkedPosts();
    return idsToString(posts);
  }

  private String unreadIds(FeedService fs) throws SQLException {
    List<Post> posts = fs.getUnreadPosts(0);
    return idsToString(posts);
  }

  private String idsToString(List<? extends HasId> haveIds) {
    StringBuilder sb = new StringBuilder();
    for (HasId i : haveIds) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(i.getId());
    }
    return sb.toString();
  }

  private JsonArray feedsArray(List<Feed> feeds) throws SQLException {
    JsonArray jab = new JsonArray();
    for (Feed f : feeds) {
      jab.add(toJson(f));
    }
    return jab;
  }

  private JsonArray itemsArray(List<Post> posts, Set<Long> bookmarks) {
    JsonArray jab = new JsonArray();
    for (Post p : posts) {
      jab.add(toJson(p, bookmarks.contains(p.getId())));
    }
    return jab;
  }

  private JsonObject toJson(Feed f) {
    java.util.Date lastAccess = f.getLastAccess();
    return new JsonObject()
      .put("id", f.getId())
      .put("title", f.getTitle())
      .put("url", f.getLocation())
      .put("site_url", f.getLocation())
      .put("is_spark", 0)
      .put("last_updated_on_time", 
        lastAccess == null ? 0 : unixtime(lastAccess.getTime()));
  }

  private JsonObject toJson(Post p, boolean bookmarked) {
    return new JsonObject()
      .put("id", p.getId())
      .put("feed_id", p.getFeed().getId())
      .put("title", p.getTitle())
      .put("html", p.getSnippet())
      .put("url", p.getLocation())
      .put("is_read", p.isRead() ? 1 : 0)
      .put("is_saved", bookmarked ? 1 : 0)
      .put("created_on_time", unixtime(p.getDate().getTime()));
  }

  private static int unixtime(long time) {
    return (int) (time / 1000L);
  }

  private static String md5(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(string.getBytes(StandardCharsets.US_ASCII));
      return String.format("%032x", new BigInteger(1, digest));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No MD5 available.", e);
    }
  }
}
