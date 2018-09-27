package fiets.filter;

import org.w3c.dom.Node;

import fiets.model.Feed;
import fiets.processors.FeedProcessor;

/**
 * Will be called by a {@link FeedProcessor} implementation with the raw post
 * of whatever type (e.g., {@link Node}).<br/>
 * Has to determine if each respective post is allowed or subject to be
 * dropped.
 */
public interface RawPostFilter {
  boolean isAllowed(Feed feed, Object rawPost);
}
