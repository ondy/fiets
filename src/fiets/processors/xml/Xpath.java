/**
 *
 */
package fiets.processors.xml;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Xpath convenience stuff.
 */
public final class Xpath {

  public static String xpathAsString(Node doc, String expression)
    throws XPathExpressionException {
    return (String) xpath(doc, expression, XPathConstants.STRING);
  }

  public static NodeList xpathAsNodes(Node doc, String expression)
    throws XPathExpressionException {
    return (NodeList) xpath(doc, expression, XPathConstants.NODESET);
  }

  private static Object xpath(Node doc, String expression, QName type)
    throws XPathExpressionException {
    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();
    XPathExpression expr = xpath.compile(expression);
    return expr.evaluate(doc, type);
  }

  private Xpath() {}
}
