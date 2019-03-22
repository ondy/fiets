package fiets.display;

import fiets.model.Post;

public class DefaultPostDisplayProvider implements PostDisplayProvider {

  @Override public PostDisplay getDisplay(Post post) {
    return new StandardPostDisplay(post);
  }

}
