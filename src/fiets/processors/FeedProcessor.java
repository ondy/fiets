package fiets.processors;

import java.util.List;

import fiets.filter.RawPostFilter;
import fiets.model.Feed;
import fiets.model.Post;

public interface FeedProcessor {
  boolean canHandle(Feed feed, String content);
  List<Post> parsePosts(Feed feed, String content, RawPostFilter filter) 
    throws Exception;
  String parseTitle(Feed feed, String content) throws Exception;
}
