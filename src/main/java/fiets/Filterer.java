package fiets;

import java.util.Collections;
import java.util.List;

import fiets.model.Filter;
import fiets.model.Post;

public class Filterer {
  public static Filterer ALL = new Filterer(Collections.emptyList());

  private List<Filter> filters;
  
  public Filterer(List<Filter> theFilters) {
    filters = theFilters;
  }

  public boolean isAllowed(Post post) {
    for (Filter f : filters) {
      if (f.getTitleMatch().matches(f.getTitle(), post.getTitle())) {
        return false;
      }
      if (f.getUrlMatch().matches(f.getUrl(), post.getLocation())) {
        return false;
      }
    }
    return true;
  }
}
