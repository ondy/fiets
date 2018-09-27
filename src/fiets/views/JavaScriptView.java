package fiets.views;

public class JavaScriptView implements View<String> {

  private String script;

  public JavaScriptView(String theScript) {
    script = theScript;
  }

  @Override public String getMimeType() {
    return "application/javascript";
  }

  @Override public String getContent() {
    return script;
  }

}
