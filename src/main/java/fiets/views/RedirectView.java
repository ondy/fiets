package fiets.views;

import fiets.PathMatch;

public class RedirectView implements View<PathMatch> {

  private PathMatch location;

  public RedirectView(PathMatch theLocation) {
    location = theLocation;
  }

  @Override public PathMatch getContent() {
    return location;
  }

  @Override public String getMimeType() {
    return null;
  }

}
