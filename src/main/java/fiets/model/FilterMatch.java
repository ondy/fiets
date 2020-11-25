package fiets.model;

import java.util.regex.Pattern;

public enum FilterMatch {
  IGNORE {
    @Override
    public boolean matches(String pattern, String content) {
      return true;
    }
  },
	REGEX {
    @Override
    public boolean matches(String pattern, String content) {
      return Pattern.compile(pattern).matcher(content).matches();
    }
  },
	STARTS_WITH {
    @Override
    public boolean matches(String pattern, String content) {
      return content.startsWith(pattern);
    }
  },
	ENDS_WITH {
    @Override
    public boolean matches(String pattern, String content) {
      return content.endsWith(pattern);
    }
  },
	CONTAINS {
    @Override
    public boolean matches(String pattern, String content) {
      return content.contains(pattern);
    }
  };
	
	public abstract boolean matches(String pattern, String content);
}
