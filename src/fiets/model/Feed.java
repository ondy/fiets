package fiets.model;

import java.time.Instant;
import java.util.Date;

// immutable
public class Feed implements HasId {
  private final long id;
  private final String location;
  private final String title;
  private final Date lastAccess;
  private final String lastStatus;
  public Feed(
    long theId, String theLocation, String theTitle, Date theLastAccess,
    String theLastStatus) {
    id = theId;
    location = theLocation;
    title = theTitle;
    lastAccess = theLastAccess == null ? null : (Date) theLastAccess.clone();
    lastStatus = theLastStatus;
  }
  public Feed(String theLocation, String theTitle, String theLastStatus) {
    this(0l, theLocation, theTitle, Date.from(Instant.EPOCH), theLastStatus);
  }
  public String getLocation() {
    return location;
  }
  public Date getLastAccess() {
    return lastAccess == null ? null : (Date) lastAccess.clone();
  }
  public long getId() {
    return id;
  }
  @Override public String toString() {
    return "Feed@" + location;
  }
  public String getTitle() {
    return title;
  }
  public String getLastStatus() {
    return lastStatus;
  }
}
