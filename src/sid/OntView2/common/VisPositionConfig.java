package sid.OntView2.common;

import javafx.application.Platform;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;


public class VisPositionConfig {
	Document doc;
	XPath xpath;
	static VisPositionConfig config = new VisPositionConfig();
	
	public void setup(String path) throws IOException, ParserConfigurationException, SAXException {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder  = domFactory.newDocumentBuilder();
        doc = builder.parse(path);
		xpath = XPathFactory.newInstance().newXPath();
	}
	
	public static void saveState(String path, VisGraph graph){
		if (graph!= null){
			try {
				new VisPositionConfig().saveStatePrivate(path, graph);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String[] restoreOntologyReasoner(String path) throws IOException, ParserConfigurationException,
                                                                            SAXException, XPathExpressionException {
        config.setup(path);
        return config.recoverGraphInfo();
	}

	public String[] recoverGraphInfo() throws XPathExpressionException {
		String s="//ontologyName/text()";
		XPathExpression expr = xpath.compile(s);
		Object ontologyName = expr.evaluate(doc, XPathConstants.STRING);

		s="//reasoner/text()";
		expr = xpath.compile(s);
		Object reasoner = expr.evaluate(doc, XPathConstants.STRING);

        s="//options/properties/text()";
        expr = xpath.compile(s);
        Object properties = expr.evaluate(doc, XPathConstants.STRING);

        s="//options/label/text()";
        expr = xpath.compile(s);
        Object label = expr.evaluate(doc, XPathConstants.STRING);

        s="//options/qualifiedNames/text()";
        expr = xpath.compile(s);
        Object qualifiedNames = expr.evaluate(doc, XPathConstants.STRING);

		return new String[]{(String) ontologyName, (String) reasoner,
                (String) properties, (String) label, (String) qualifiedNames};
	}
	
	public static void restoreState(VisGraph graph){
		if (graph!= null && config!=null){
			try {
				config.recoverVisInfo(graph);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void recoverVisInfo(VisGraph graph) throws XPathExpressionException{
		for (Entry<String, Shape> entry : graph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			String key = entry.getKey();

			recoverShapePos(shape, key);
			recoverShapeState(shape, key);
			recoverShapeVisibility(shape, key);
		}
		for (Entry<String, Shape> entry : graph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if (shape.isVisible()) shape.updateHiddenDescendants();
		}
		for (VisConnector connector : graph.connectorList) {
			connector.visible = (connector.from.visible && connector.to.visible);
		}
		graph.clearDashedConnectorList();
		graph.addDashedConnectors();
		graph.paintframe.setStateChanged(true);
		Platform.runLater(graph.paintframe.getRelaxerRunnable());

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
		// right state
		String s="//"+search+"/rightState/text()";
		XPathExpression expr = xpath.compile(s);
        Object result = expr.evaluate(doc, XPathConstants.STRING);
        shape.setState(mapStateRight((String) result));

		// left state
		s="//"+search+"/leftState/text()";
		expr = xpath.compile(s);
		result = expr.evaluate(doc, XPathConstants.STRING);
		shape.setLeftState(mapStateLeft((String) result));

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

	
	public int mapStateRight(String stateString){
		if (stateString.equals("closed")) 		return Shape.CLOSED;
		if (stateString.equals("open")) 		return Shape.OPEN;
		if (stateString.equals("partially"))	return Shape.PARTIALLY_CLOSED;
		if (stateString.equals("0"))			return Shape.CLOSED;
		if (stateString.equals("1"))			return Shape.OPEN;
		if (stateString.equals("2"))			return Shape.PARTIALLY_CLOSED;
		return Shape.OPEN;
	}

	public int mapStateLeft(String stateString){
		if (stateString.equals("3"))			return Shape.LEFTCLOSED;
		if (stateString.equals("4"))			return Shape.LEFTOPEN;
		if (stateString.equals("5"))			return Shape.LEFT_PARTIALLY_CLOSED;
		return Shape.LEFTOPEN;
	}

	public boolean mapBool( String boolString){
		if (boolString.equals("true")) return true;
        return !boolString.equals("false");
    }

	private void saveStatePrivate(String resourcePath, VisGraph graph) throws IOException{
         if (resourcePath != null){ 
        	 new File(resourcePath).createNewFile();
        	 boolean anon;
        	 BufferedWriter out = new BufferedWriter(new FileWriter(resourcePath));
			 out.write("<?xml version=\"1.0\" ?>\n");
			 out.write("<root>\n");

			 out.write("\t<ontologyName>" + graph.paintframe.getActiveOntologySource() + "</ontologyName>\n");
			 out.write("\t<reasoner>" + graph.paintframe.getReasoner().getReasonerName() + "</reasoner>\n");

             out.write("\t<options>\n");
             out.write("\t\t<properties>" + graph.paintframe.nTopPanel.getPropertiesCheckBox().isSelected() + "</properties>\n");
             out.write("\t\t<label>" + graph.paintframe.nTopPanel.getRenderLabel().isSelected() + "</label>\n");
             out.write("\t\t<qualifiedNames>" + graph.paintframe.nTopPanel.getQualifiedNames().isSelected() + "</qualifiedNames>\n");
             out.write( "\t</options>\n");

			 for (Entry<String, Shape> entry : graph.shapeMap.entrySet()){
				 Shape shape = entry.getValue();
				 String key;
				 anon = false;
				 if ((shape instanceof VisConstraint)|| (shape instanceof VisClass)&&(shape.asVisClass().isAnonymous)) {
					 anon = true;
					 key = "Anon id=\""+escapeXML(entry.getKey())+"\"";
				 }
				 else {
					 key = "Named id=\""+escapeXML(entry.getKey())+"\"";

				 }
					  out.write("\t"); out.write("<"+key+">\n");
						  	out.write("\t"); out.write("\t");
						  	out.write("<posy>"+shape.getPosY()+"</posy>\n");
						  	out.write("\t"); out.write("\t");
						  	out.write("<rightState>"+shape.getState()+"</rightState>\n");
							out.write("\t"); out.write("\t");
				 			out.write("<leftState>"+shape.getLeftState()+"</leftState>\n");
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
