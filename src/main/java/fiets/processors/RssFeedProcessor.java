package fiets.processors;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fiets.Filterer;
import fiets.model.Feed;
import fiets.model.Post;
import fiets.processors.xml.Dom;
import fiets.processors.xml.Xpath;
import fiets.sources.HttpFeedSource;

public class RssFeedProcessor implements FeedProcessor {

  private static final Logger log = LogManager.getLogger();

  @Override public boolean canHandle(Feed feed, String content) {
    if (!HttpFeedSource.isHttpSource(feed) || content == null) {
      return false;
    }
    try {
      content = Xml.dropSignature(content);
      content = Xml.dropComments(content.trim());
      return content.matches("(?s)^<([a-zA-Z0-9]+\\:)?(rss|RDF).*");
    } catch (RuntimeException e) {
      log.debug(e, e);
      return false;
    }
  }

  @Override public String parseTitle(Feed feed, String content)
    throws SAXException, IOException,
    ParserConfigurationException, XPathExpressionException {
    Document doc = Dom.parse(content);
    return Xpath.xpathAsString(doc, "//channel/title").orElse("-no-title");
  }

  @Override public List<Post> parsePosts(
    Feed feed, String content, Filterer ff)
    throws XPathExpressionException, SAXException,
      IOException, ParserConfigurationException, ParseException {
    Document doc = Dom.parse(content);
    NodeList items = Xpath.xpathAsNodes(doc, "//item");
    List<Post> result = new ArrayList<>();
    int num = items.getLength();
    if (num > 0) {
      for (int i = 0; i < num; i++) {
        Node item = items.item(i);
        String title = Xpath.xpathAsString(item, "title").orElse("-no title");
        String link = Xpath.xpathAsString(item, "link").orElse("-no-link-");
        if (link.trim().length() == 0) {
          link = Xpath.xpathAsString(item, "guid").orElse("-no-guid");
        }
        String description = Xpath.xpathAsString(item, "description").orElse("");
        Date date = Xpath.xpathAsString(item, "pubDate")
            .map(dateString -> Xml.parseDate(dateString))
            .orElse(new Date());
        Post post = new Post(0l, link, date, title, description, false, feed);
        if (ff.isAllowed(post)) {
          result.add(post);
        }
      }
    }
    return result;
  }
}
