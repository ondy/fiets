package fiets.views;

import jodd.json.JsonObject;

public class JsonView implements View<String> {

  private final JsonObject json;

  public JsonView(JsonObject theJson) {
    json = theJson;
  }

  @Override public String getMimeType() {
    return "application/json";
  }

  @Override public String getContent() {
    return json.toString();
  }

}
