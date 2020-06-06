package fiets.processors;

import java.util.List;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Post;

public interface FeedProcessor {
  boolean canHandle(Feed feed, String content);
  List<Post> parsePosts(Feed feed, String content, Filterer ff) 
    throws Exception;
  String parseTitle(Feed feed, String content) throws Exception;
}
