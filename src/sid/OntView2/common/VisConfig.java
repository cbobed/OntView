package sid.OntView2.common;

import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

public class VisConfig {

	static String BASE ="plugins/sid/OntView/Protege/";

	private static VisConfig instance = null;
	DocumentBuilderFactory domFactory;
	Document doc;
	XPath xpath;

//	enum ClassExpressionsOption {ALL, ONLY_CLASSES, ONLY_DOMAIN, ONLY_RANGE, DOMAIN_RANGE}; 
//	public static ClassExpressionsOption visualizeClassExpressions = ClassExpressionsOption.ALL;
	
	public static boolean APPLY_RENAMING_DEBUG_PURPOSES=false; 
	
	private VisConfig() throws ParserConfigurationException, SAXException, IOException{
		  DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	      domFactory.setNamespaceAware(true); 
	      DocumentBuilder builder = domFactory.newDocumentBuilder();
	      ClassLoader c;
	      if (getClass().getClassLoader() == null)
	    	  c = ClassLoader.getSystemClassLoader();
	      else 
	    	  c = getClass().getClassLoader();
	      try {
			doc = builder.parse(c.getResource("visconfig.xml").toURI().toString());
		  } catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      
	      xpath = XPathFactory.newInstance().newXPath();
	}
	private synchronized static void createInstance() throws ParserConfigurationException, SAXException, IOException{
		if ((instance) == null) {
			instance = new VisConfig();
		}
	}

	public static VisConfig getInstance() throws ParserConfigurationException, SAXException, IOException{
		if (instance == null) 
			createInstance();
		return instance;
	}
	
	public void connectorWidth() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//connector/width/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnector.width =  Float.parseFloat((String) result);
	}
	
	public void connectorColor() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//connector/color/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnector.color =  Color.BLUE;
	}
	
	public void dashedConnectorColor() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//dashedConnector/color/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnectorDashed.color = Color.GREEN;
	}
	
	
	public void setConstants() {
		try {
			connectorColor();
			connectorWidth();
			dashedConnectorColor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
}
