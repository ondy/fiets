package fiets.model;

import java.util.Date;

// immutable
public class FeedInfo {
  private final Feed feed;
  private final int numUnread;
  private final int numRead;
  private final Date mostRecentPost;
  public FeedInfo(Feed theFeed, int theNumUnread, int theNumRead, 
    Date theMostRecentPost) {
    feed = theFeed;
    numUnread = theNumUnread;
    numRead = theNumRead;
    mostRecentPost = theMostRecentPost == null 
      ? new Date() : (Date) theMostRecentPost.clone();
  }
  public int getNumUnread() {
    return numUnread;
  }
  public int getNumRead() {
    return numRead;
  }
  public Date getMostRecentPost() {
    return (Date) mostRecentPost.clone();
  }
  public Feed getFeed() {
    return feed;
  }
}
