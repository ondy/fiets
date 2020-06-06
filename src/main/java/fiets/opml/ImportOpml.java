package fiets.opml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fiets.FeedService;
import fiets.db.Database;
import fiets.processors.xml.Dom;
import fiets.processors.xml.Xpath;

public class ImportOpml {

  public static void main(String[] args) throws Exception {
    String filename = args[0];
    List<String> urls = parseXmlUrlsFromOpml(filename);
    try (Database db = new Database()) {
      new FeedService(db).addFeeds(urls);
    }
  }

  private static List<String> parseXmlUrlsFromOpml(String filename)
    throws FileNotFoundException, SAXException, IOException,
    ParserConfigurationException, XPathExpressionException {
    InputSource source = new InputSource(new InputStreamReader(
      new FileInputStream(filename), StandardCharsets.UTF_8));
    source.setEncoding(StandardCharsets.UTF_8.name());
    Document doc = Dom.parse(source);
    NodeList outlines = Xpath.xpathAsNodes(doc, "//outline[@xmlUrl]");
    int len = outlines.getLength();
    List<String> feeds = new ArrayList<>();
    for (int i = 0; i < len; i++) {
      String xmlUrl =
        outlines.item(i).getAttributes().getNamedItem("xmlUrl").getNodeValue();
      feeds.add(xmlUrl);
    }
    return feeds;
  }
}
