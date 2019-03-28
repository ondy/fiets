package fiets.display;

import fiets.model.Post;

public interface PostDisplayProvider {
  PostDisplay getDisplay(Post post);
}
