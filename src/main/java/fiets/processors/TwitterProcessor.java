package fiets.processors;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Post;
import fiets.sources.HttpFeedSource;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterProcessor implements FeedProcessor {

  private ContentUnfolder unfolder = new ContentUnfolder();

  @Override public boolean canHandle(Feed feed, String content) {
    return feed.getLocation().toLowerCase(Locale.ROOT)
      .startsWith("twitter://");
  }

  private static Twitter initTwitter(Feed feed) {
    try {
      Map<String, List<String>> qs = HttpFeedSource
        .parseQueryString(feed.getLocation(), StandardCharsets.UTF_8);
      String consumerKey = qs.get("consumerKey").get(0);
      String consumerSecret = qs.get("consumerSecret").get(0);
      String token = qs.get("token").get(0);
      String tokenSecret = qs.get("tokenSecret").get(0);
      Configuration conf = new ConfigurationBuilder()
        .setOAuthAccessToken(token)
        .setOAuthAccessTokenSecret(tokenSecret)
        .setOAuthConsumerKey(consumerKey)
        .setOAuthConsumerSecret(consumerSecret)
        .setHttpConnectionTimeout(5000)
        .setHttpReadTimeout(5000)
        .setTweetModeExtended(true).build();
      Twitter t = new TwitterFactory(conf).getInstance();
      return t;
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 not supported.", e);
    }
  }

  @Override public List<Post> parsePosts(Feed feed, String content) throws Exception {
    Twitter tw = initTwitter(feed);
    ResponseList<Status> timeline = tw.getHomeTimeline(new Paging(1, 50));
    List<Post> result = new ArrayList<>();
    for (Status status : timeline) {
      Post post = toPost(feed, status);
      result.add(post);
    }
    return result;
  }

  private Post toPost(Feed feed, Status status) {
    String url = "https://twitter.com/" + status.getUser().getScreenName()
        + "/status/" + status.getId();
    String title = status.getUser().getScreenName() + ": " + status.getText();
    String snippet = status.getText();
    snippet = unfolder.unfoldUrls(snippet, "https://t.co/");
    Date date = status.getCreatedAt();
    return new Post(0l, url, date, title, snippet, false, feed);
  }

  @Override public String parseTitle(Feed feed, String content) {
    String url = feed.getLocation();
    int namePos = url.indexOf("://") + 3;
    int endPos = url.indexOf('?', namePos);
    return "Twitter Feed " + url.substring(namePos, endPos);
  }
}
