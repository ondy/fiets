package fiets.processors;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Post;
import fiets.sources.HttpFeedSource;
import jodd.http.HttpMultiMap;
import jodd.http.HttpRequest;
import jodd.jerry.Jerry;
import jodd.jerry.JerryFunction;
import jodd.json.JsonObject;
import jodd.json.JsonParser;
import jodd.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FacebookProcessor implements FeedProcessor {

  private static final Logger log = LogManager.getLogger();

  private static final String FB_HOST = "mobile.facebook.com";

  @Override public boolean canHandle(Feed feed, String content) {
    try {
      return new URL(feed.getLocation()).getHost().equals(FB_HOST);
    } catch (MalformedURLException e) {
      log.error(e, e);
      return false;
    }
  }

  @Override public String parseTitle(Feed feed, String content) {
    return "Facebook - " + Jerry.jerry(content).find("title").text();
  }

  @Override public List<Post> parsePosts(
    Feed feed, String content, Filterer ff) {
    Jerry articles = Jerry.jerry(content).find("article");
    List<Post> result = new ArrayList<>();
    //noinspection Convert2Lambda
    articles.each(new JerryFunction() {
      @Override
      public Boolean onNode(Jerry $this, int index) {
        String title = $this.find("h3").text();
        Jerry a = $this.find("a[href^='/story.php']");
        String link = feed.getLocation();
        if (a.length() > 0) {
          link = fixUrl(a.attr("href"));
        }
        String description = $this.find("p").text();
        String mainTitle = $this.find("h3 a").text();
        if (title.equals(mainTitle)) {
          title = title + " - " + description.substring(0, Math.min(50, description.length()));
        } else {
          title = StringUtil.insert(title, " - ", mainTitle.length());
        }
        Date date = extractDate($this);

        Post post = new Post(0L, link, date, title, description, false, feed);
        if (ff.isAllowed(post)) {
          result.add(post);
        }
        return true;
      }
    });
    return result;
  }

  private String fixUrl(String href) {
    if (href.startsWith("/")) {
      href = "https://" + FB_HOST + href;
    }
    HttpMultiMap<String> query = HttpRequest.get(href).query();
    String fbid = query.get("story_fbid");
    String id = query.get("id");
    return String.format("https://%s/story.php?story_fbid=%s&id=%s",
            FB_HOST, fbid, id);
  }

  private static Date extractDate(Jerry article) {
    try {
      String ft = article.eq(0).attr("data-ft");
      JsonObject json = JsonParser.create().parseAsJsonObject(ft);
      JsonObject insights = json.getJsonObject("page_insights");
      String first = insights.fieldNames().iterator().next();
      Long time = insights.getJsonObject(first).getJsonObject("post_context").getLong("publish_time");
      return new Date(time*1000);
    } catch (RuntimeException e) {
      return new Date();
    }
  }

}
