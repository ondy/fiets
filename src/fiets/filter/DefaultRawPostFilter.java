package fiets.filter;

import org.w3c.dom.Node;

import fiets.model.Feed;

public class DefaultRawPostFilter implements RawPostFilter {

  @Override public boolean isAllowed(Feed feed, Object fmt) {
    if (fmt instanceof Node) {
      return isAllowed(feed, (Node) fmt);
    }
    return true;
  }

  /**
   * To be overridden by custom filters.
   */
  public boolean isAllowed(Feed feed, Node node) {
    return true;
  }
}
