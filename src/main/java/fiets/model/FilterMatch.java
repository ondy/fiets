package fiets.model;

import java.util.regex.Pattern;

public enum FilterMatch {
  IGNORE("is ignored") {
    @Override
    public boolean matches(String pattern, String content) {
      return true;
    }
	
    @Override
    public String displayText(String type, String value) {
	  return String.format("ignore %s", type);
    }
  },
	REGEX("matches regex") {
    @Override
    public boolean matches(String pattern, String content) {
      return Pattern.compile(pattern).matcher(content).matches();
    }
  },
	STARTS_WITH("starts with") {
    @Override
    public boolean matches(String pattern, String content) {
      return content.startsWith(pattern);
    }
  },
	ENDS_WITH("ends with") {
    @Override
    public boolean matches(String pattern, String content) {
      return content.endsWith(pattern);
    }
  },
	CONTAINS("contains") {
    @Override
    public boolean matches(String pattern, String content) {
      return content.contains(pattern);
    }
  };

  private String text;

  FilterMatch(String text) {
    this.text = text;
  }

  public abstract boolean matches(String pattern, String content);

  public String displayText(String type, String value) {
	  return String.format("%s %s %s", type, text, value);
  }
}
