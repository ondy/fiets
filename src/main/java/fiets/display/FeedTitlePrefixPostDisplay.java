package fiets.display;

import fiets.model.Post;

/**
 * Post display that adds a 'list-group-item-&lt;context>' to the posts
 * list item to color the background. 
 */
public class FeedTitlePrefixPostDisplay extends StandardPostDisplay {

  public FeedTitlePrefixPostDisplay(Post thePost) {
    super(thePost);
  }

  @Override public String getTitle() {
    return getPost().getFeed().getTitle() + " - " + getPost().getTitle();
  }
}
