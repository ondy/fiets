package fiets.display;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fiets.model.Post;

public class StandardPostDisplay implements PostDisplay {

  private Post post;

  public StandardPostDisplay(Post thePost) {
    post = thePost;
  }

  @Override public String getAdditionalPostClass() {
    return "";
  }

  @Override public String getAdditionalLinks() {
    return "";
  }

  @Override public String getTitle() {
    return post.getTitle();
  }

  @Override public String getSnippet() {
    String snippet = post.getSnippet();
    if (snippet == null || snippet.trim().length() == 0) {
      snippet = post.getTitle();
    }
    return snippet;
  }

  @Override public String getDate() {
    return fmtDate(post.getDate());
  }

  public static String fmtDate(Date date) {
    return date == null ? "--" :
      new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH).format(date);
  }

  @Override public String getFeedTitle() {
    return post.getFeed().getTitle();
  }

  protected Post getPost() {
    return post;
  }

  @Override public String getLocation() {
    return post.getLocation();
  }

}
