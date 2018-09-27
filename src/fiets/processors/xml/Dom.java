package fiets.processors.xml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class Dom {
  private static final DocumentBuilderFactory DOM_FACTORY =
      DocumentBuilderFactory.newInstance();

  public static Document parse(InputSource source)
    throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilder builder = DOM_FACTORY.newDocumentBuilder();
    return builder.parse(source);
  }

  public static Document parse(String string)
    throws SAXException, IOException, ParserConfigurationException {
    InputSource source = new InputSource(new StringReader(string));
    return parse(source);
  }

  private Dom() {}
}
