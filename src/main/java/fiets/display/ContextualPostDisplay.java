package fiets.display;

import fiets.model.Post;

/**
 * Post display that adds a 'list-group-item-&lt;context>' to the posts
 * list item to color the background. 
 */
public class ContextualPostDisplay extends StandardPostDisplay {

  private String context;

  public ContextualPostDisplay(Post thePost, String theContext) {
    super(thePost);
    context = theContext;
  }

  @Override public String getAdditionalPostClass() {
    return "list-group-item-" + context;
  }
}
