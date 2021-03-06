package fiets.model;

import java.util.Optional;

// immutable
public class Filter {
	private final long id;
	private final String url;
	private final FilterMatch urlMatch;
	private final String title;
	private final FilterMatch titleMatch;
	private long matchCount;
  public Filter(long theId, String url, FilterMatch urlMatch, String title, FilterMatch titleMatch, long matchCount) {
    this.id = theId;
    this.url = url;
    this.urlMatch = urlMatch;
    this.title = title;
    this.titleMatch = titleMatch;
    this.matchCount = matchCount;
  }
  public long getId() {
    return id;
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

  public long getMatchCount() {
    return matchCount;
  }

  public void incMatchCount() {
    matchCount = matchCount + 1;
  }
}
