package fiets.processors;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Post;
import jodd.jerry.Jerry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FacebookProcessor implements FeedProcessor {

  private static final Logger log = LogManager.getLogger();

  private static final String FB_HOST = "www.facebook.com";

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
    List<Post> result = new ArrayList<>();

    Jerry jerry = Jerry.jerry(content);
    int maxLevel = findMaxHLevel(jerry);
    Jerry titles = jerry.find("h" + maxLevel);
    titles.forEach(title -> {
      Jerry parent = title.parent();
      for (int i = 0; i < 20; i++) {
        int len = parent.text().length();
        if (len > 100) {
          Post p = createPost(title, parent, feed);
          if (ff.isAllowed(p)) {
            result.add(p);
          }
          break;
        }
        parent = parent.parent();
      }
    });

    if (result.isEmpty()) {
      result.add(Process.errorPost(feed, "No posts found!"));
    }
    return result;
  }

  private Date tryFindDate(Jerry post) {
    try {
      String utime = post.find("[data-utime]").attr("data-utime");
      if (utime != null) {
        return new Date(Long.parseLong(utime) * 1000);
      } else {
        return new Date();
      }
    } catch (RuntimeException e) {
      return new Date();
    }
  }

//    String content = HttpFeedSource.readUrlContent("https://www.facebook.com/westfaelischer.anzeiger/posts/?_fb_noscript=1");
  private Post createPost(Jerry title, Jerry post, Feed feed) {
    String link = tryFindUrl(title, post);
    Date date = tryFindDate(post);
    post.find("[class^='timestamp']").remove();
    String titleString = title.text();
    if (titleString.length() < 10) {
      titleString = post.text().substring(0, 100);
    }
    return new Post(0L, link, date, titleString, post.text(), false, feed);
  }

  private String tryFindUrl(Jerry title, Jerry parent) {
    String urlCandidate = null;
    Jerry a = title.find("a");
    if (a.length() > 0) {
      urlCandidate = a.attr("href");
    }
    a = parent.find("a[href^='/']");
    if (a.length() > 0) {
      urlCandidate = a.attr("href");
    }
    if (urlCandidate == null) {
      urlCandidate = "https://" + FB_HOST + "?" + System.currentTimeMillis();
    } else if (!urlCandidate.startsWith("http://") && !urlCandidate.startsWith("https://")) {
      urlCandidate = "https://" + FB_HOST + urlCandidate;
    }
    return urlCandidate;
  }

  private int findMaxHLevel(Jerry jerry) {
    int maxCount = 0;
    int maxLevel = 0;
    for (int i = 1; i <= 6; i++) {
      int thisCount = jerry.find("h" + i).length();
      if (thisCount > maxCount) {
        maxCount = thisCount;
        maxLevel = i;
      }
    }
    return maxLevel;
  }
}
