package fiets.model;

import java.util.Optional;

// immutable
public class Filter {
	
	private final String url;
	private final FilterMatch urlMatch;
	private final String title;
	private final FilterMatch titleMatch;
  public Filter(String url, FilterMatch urlMatch, String title, FilterMatch titleMatch) {
    this.url = url;
    this.urlMatch = urlMatch;
    this.title = title;
    this.titleMatch = titleMatch;
  }
  public String getUrl() {
    return url;
  }
  public FilterMatch getUrlMatch() {
    return urlMatch;
  }
  public String getTitle() {
    return title;
  }
  public FilterMatch getTitleMatch() {
    return titleMatch;
  }
}
