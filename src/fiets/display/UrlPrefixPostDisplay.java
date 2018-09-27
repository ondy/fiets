package fiets.display;

import fiets.model.Post;

public class UrlPrefixPostDisplay extends StandardPostDisplay {

  private String prefix;

  public UrlPrefixPostDisplay(String thePrefix, Post thePost) {
    super(thePost);
    prefix = thePrefix;
  }

  @Override public String getLocation() {
    return prefix + super.getLocation();
  }
}
