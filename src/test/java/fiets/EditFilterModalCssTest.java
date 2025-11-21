package fiets;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class EditFilterModalCssTest {

  @Test
  public void editFilterModalIsNotForcedHidden() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/styles.css");
    String css = new String(Files.readAllBytes(cssPath), StandardCharsets.UTF_8);

    Pattern modalRule = Pattern.compile("#edit-filter-modal\\s*\\{[^}]*}", Pattern.MULTILINE);
    Matcher matcher = modalRule.matcher(css);
    if (matcher.find()) {
      String rule = matcher.group().toLowerCase();
      assertFalse("Edit filter modal rule must not force it hidden", rule.contains("display"));
    }
  }
}
