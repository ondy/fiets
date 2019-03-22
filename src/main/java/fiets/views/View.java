package fiets.views;

public interface View<ContentType> {
  String getMimeType();
  ContentType getContent();
}
