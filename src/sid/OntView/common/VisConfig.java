package sid.OntView.common;
import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;

import java.awt.Color;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.xml.sax.SAXException;

public class VisConfig {

	static String BASE ="plugins/sid/OntView/Protege/";
	private static VisConfig instance = null;
	DocumentBuilderFactory domFactory;
	Document doc;
	XPath xpath;
	
	
	HashMap<String, Color> map;

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
	      map = new  HashMap<String, Color>();
	      mapColor();
	      
	      xpath = XPathFactory.newInstance().newXPath();
	}
	private synchronized static void createInstance() throws ParserConfigurationException, SAXException, IOException{
		if ((instance) == null) {
			instance = new VisConfig();
		}
	}
	
	private void mapColor (){
		map.put("blue",Color.blue);
		map.put("red",Color.red);
		map.put("black",Color.black);
		map.put("cyan",Color.cyan);
		map.put("darkGray",Color.darkGray);
		map.put("orange",Color.orange);
		map.put("lightGray",Color.lightGray);
		map.put("magenta", Color.magenta);
		map.put("yellow", Color.yellow);
		map.put("green", Color.green);
	}
	public static VisConfig getInstance() throws ParserConfigurationException, SAXException, IOException{
		if (instance == null) 
			createInstance();
		return instance;
	}
	
	public void connectorWidth() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//connector/width/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnector.width =  Float.valueOf((String) result);
	}
	
	public void connectorColor() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//connector/color/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnector.color =  map.get((String)result);
	}
	
	public void dashedConnectorColor() throws XPathExpressionException{
		XPathExpression expr = xpath.compile("//dashedConnector/color/text()");
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        VisConnectorDashed.color =  map.get((String)result);
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
