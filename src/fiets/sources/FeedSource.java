package fiets.sources;

import fiets.model.Feed;

public interface FeedSource {
  boolean canHandle(Feed feed);
  String process(Feed feed);
}
