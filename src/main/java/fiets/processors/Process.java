package fiets.processors;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Filter;
import fiets.model.Post;
import fiets.sources.FeedSource;
import fiets.sources.HttpFeedSource;

public class Process {
  private static final Process PROCESS = new Process();

  static {
    PROCESS.register(new HttpFeedSource());
    PROCESS.register(new RssFeedProcessor());
    PROCESS.register(new AtomFeedProcessor());
    PROCESS.register(new TwitterProcessor());
    PROCESS.register(new FacebookProcessor());
  }

  public static List<Post> parsePosts(
    Feed feed, Filterer ff) throws Exception {
    String input = PROCESS.preprocess(feed);
    return PROCESS.getParser(feed, input).parsePosts(feed, input, ff);
  }

  public static String parseTitle(Feed feed) throws Exception {
    String input = PROCESS.preprocess(feed);
    return PROCESS.getParser(feed, input).parseTitle(feed, input);
  }

  private static final Logger log = LogManager.getLogger();
  private final List<FeedProcessor> processors = new LinkedList<>();
  private final List<FeedSource> sources = new LinkedList<>();

  private String preprocess(Feed feed) {
    for (FeedSource pre : sources) {
      if (pre.canHandle(feed)) {
        return pre.process(feed);
      }
    }
    return null;
  }

  private FeedProcessor getParser(Feed feed, String findFor) {
    for (FeedProcessor f : processors) {
      if (f.canHandle(feed, findFor)) {
        return f;
      }
    }
    log.debug(findFor);
    throw new IllegalArgumentException(
      "No matching parser found for " + feed.getLocation());
  }

  private void register(FeedSource source) {
    sources.add(source);
  }

  private void register(FeedProcessor processor) {
    processors.add(processor);
  }

  public static void registerProcessor(FeedProcessor processor) {
     PROCESS.register(processor);
  }

  public static Post errorPost(Feed f, String title) {
    return errorPost(f, title, null);
  }

  public static Post errorPost(Feed f, String title, Throwable t) {
    Date d = new Date();
    Post post = new Post(0L, f.getLocation() + "?ts=" + d.getTime(), d,
            String.format("Error: %s - %s", f.getTitle(), title),
            t == null ? "" : t.getMessage(), false, f);
    return post;
  }
}
