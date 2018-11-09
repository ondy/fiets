package fiets.model;

import java.util.Date;

// immutable
public class Post implements HasId {
  private final long id;
  private final String location;
  private final Date date;
  private final String title;
  private final String snippet;
  private final boolean read;
  private final Feed feed;
  public Post(long theId, String theLocation, Date theDate, String theTitle,
    String theSnippet, boolean theRead, Feed theFeed) {
    id = theId;
    location = theLocation;
    date = theDate == null ? null : (Date) theDate.clone();
    title = theTitle;
    snippet = theSnippet;
    read = theRead;
    feed = theFeed;
  }
  public Post(long theId, Post from) {
    this(theId, from.location, from.date, 
      from.title, from.snippet, from.read, from.feed);
  }

  public String getLocation() {
    return location;
  }

  public Date getDate() {
    return (Date) date.clone();
  }

  public String getTitle() {
    return title;
  }

  public String getSnippet() {
    return snippet;
  }

  public long getId() {
    return id;
  }

  public boolean isRead() {
    return read;
  }

  @Override public String toString() {
    return "Post:" + title;
  }
  public Feed getFeed() {
    return feed;
  }
}
