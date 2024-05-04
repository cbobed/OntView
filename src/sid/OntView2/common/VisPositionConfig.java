package sid.OntView2.common;


import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;


public class VisPositionConfig {
   
//	private static VisPositionConfig instance = null;
	DocumentBuilderFactory domFactory;
	Document doc;
	XPath xpath;
	
	
	HashMap<String, Color> map;

	
	public void setup(String path){
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	      domFactory.setNamespaceAware(true); 
	      DocumentBuilder builder;
	      try {
	    	builder = domFactory.newDocumentBuilder();
	    	
			doc = builder.parse(path);
		  } 
	      catch (Exception e) {

	    	  e.printStackTrace();
		  }	
	      map = new  HashMap<String, Color>();
	      mapColor();
	      
	      xpath = XPathFactory.newInstance().newXPath();
	}
	
	public static void saveState(String path, VisGraph graph){
		if (graph!= null){
			try {
				new VisPositionConfig().saveStatePriv(path, graph);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void restoreState(String path, VisGraph graph){
		if (graph!= null){
			VisPositionConfig config = new VisPositionConfig();
			config.setup(path);
			try {
				config.recoverVisInfo(graph);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void recoverVisInfo(VisGraph graph) throws XPathExpressionException{

		for (Entry<String, Shape> entry : graph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
//				String key = escapeXML(entry.getKey());
				String key = entry.getKey();

				recoverShapePos(shape,key);
				recoverShapeState(shape, key);
				recoverShapeVisibility(shape, key);
		}
		for ( VisConnector connector : graph.connectorList){
//			recoverConnectorVisibility(connector);
			connector.visible = (connector.from.visible && connector.to.visible);
		}
        graph.clearDashedConnectorList();
        graph.addDashedConnectors();
        
	}
	
	public void recoverShapePos(Shape shape, String key) throws XPathExpressionException{
		String search;
		if ((shape instanceof VisConstraint)|| (shape instanceof VisClass)&&(shape.asVisClass().isAnonymous)) {
			search = "Anon[@id=\'"+key+"\']";
		}
		else {
			search = "Named[@id='"+key+"']";
		}		
		
		String s="//"+search+"/posy/text()";
		XPathExpression expr = xpath.compile(s);
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        shape.setPosY(Integer.parseInt((String) result));
        
	}

	public void recoverShapeState(Shape shape, String key) throws XPathExpressionException{
		String search;
		if ((shape instanceof VisConstraint)|| (shape instanceof VisClass)&&(shape.asVisClass().isAnonymous)) {
			search = "Anon[@id='"+key+"']";
		 }
		 else {
			search = "Named[@id='"+key+"']";
		 }		
		String s="//"+search+"/state/text()";
		XPathExpression expr = xpath.compile(s);
        Object result = expr.evaluate(doc, XPathConstants.STRING);
//        if (mapState((String) result) == Shape.CLOSED)	shape.close();
//        if (mapState((String) result) == Shape.OPEN)	shape.open();
        shape.setState(mapState((String) result));
        
	}
	
	public void recoverShapeVisibility(Shape shape, String key) throws XPathExpressionException{
		
		String search;
		if ((shape instanceof VisConstraint)|| (shape instanceof VisClass)&&(shape.asVisClass().isAnonymous)) {
			search = "Anon[@id='"+key+"']";
		 }
		 else {
			search = "Named[@id='"+key+"']";
		 }		
		String s="//"+search+"/visible/text()";
		XPathExpression expr = xpath.compile(s);
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        shape.visible= mapBool((String)result);
        
	}
	
	
	private void mapColor (){
		map.put("blue",Color.blue);
		map.put("red",Color.red);
		map.put("black",Color.black);
		map.put("cyan",Color.cyan);
		map.put("darkGray",Color.darkGray);
		map.put("orange",Color.orange);
		map.put("lightGray",Color.lightGray);
	}
	
	public int mapState(String stateString){
		if (stateString.equals("closed")) 		return Shape.CLOSED;
		if (stateString.equals("open")) 		return Shape.OPEN;
		if (stateString.equals("partially"))	return Shape.PARTIALLY_CLOSED;
		if (stateString.equals("0"))			return Shape.CLOSED;
		if (stateString.equals("1"))			return Shape.OPEN;
		if (stateString.equals("2"))			return Shape.PARTIALLY_CLOSED;
		return Shape.OPEN;
	}

	public boolean mapBool( String boolString){
		if (boolString.equals("true")) return true;
		if (boolString.equals("false")) return false;
		return true;
	}
	
	
	
	private void saveStatePriv(String resourcePath, VisGraph graph) throws IOException{
         if (resourcePath != null){ 
        	 new File(resourcePath).createNewFile();
        	 boolean anon =false;
        	 BufferedWriter out = new BufferedWriter(new FileWriter(new File(resourcePath)));
			 out.write("<?xml version=\"1.0\" ?>\n");
			 out.write("<root>\n");
			 for (Entry<String, Shape> entry : graph.shapeMap.entrySet()){
				 Shape shape = entry.getValue();
				 String key;
				 anon = false;
				 if ((shape instanceof VisConstraint)|| (shape instanceof VisClass)&&(shape.asVisClass().isAnonymous)) {
					 anon = true;
					 key = "Anon id=\""+escapeXML(entry.getKey())+"\"";
				 }
				 else {
//					 key = escapeXML(entry.getKey()); 
//					 String[] splittedString =key.split("#");
//					 key = splittedString[splittedString.length-1];
					 key = "Named id=\""+escapeXML(entry.getKey())+"\"";

				 }
					  out.write("\t"); out.write("<"+key+">\n");
						  	out.write("\t"); out.write("\t");
						  	out.write("<posy>"+shape.getPosY()+"</posy>\n");
						  	out.write("\t"); out.write("\t");
						  	out.write("<state>"+shape.getState()+"</state>\n");
						  	out.write("\t"); out.write("\t");
						  	out.write("<visible>"+shape.visible+"</visible>\n");
					  out.write("\t"); 
					  if (!anon) 
						  out.write("</Named>\n");
					  else 
						  out.write("</Anon>\n");
			 }
			 out.write("</root>\n");
			 out.close();
		 }
	 }
	 

	private String escapeXML(String inString){
		 String escapedString = inString.replaceAll("&,","&amp;");
		 escapedString = escapedString.replaceAll("<", "&lt;");
		 escapedString = escapedString.replaceAll(">", "&gt;");
		 escapedString = escapedString.replaceAll("\"", "&quot;");
		 return escapedString;
	 }
}
