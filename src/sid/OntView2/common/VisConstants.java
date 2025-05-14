package sid.OntView2.common;

public class VisConstants {

	public static final String NONECOMBOOPTION = "None";
	public static final String KCECOMBOOPTION1 = "KCE 10";
	public static final String KCECOMBOOPTION2 = "KCE 20";
	public static final String PAGERANKCOMBOOPTION1 = "PageRank 10";
	public static final String PAGERANKCOMBOOPTION2 = "PageRank 20";
	public static final String RDFRANKCOMBOOPTION1 = "RDFRank 10";
	public static final String RDFRANKCOMBOOPTION2 = "RDFRank 20";
	public static final String CUSTOMCOMBOOPTION3 = "Custom";
    public static final String STEPSTRATEGY_RDFRANK = "RDFRank";
    public static final String STEPSTRATEGY_RDF_LEVEL_LR = "RDFRank + Level: left to right";
    public static final String STEPSTRATEGY_RDF_LEVEL_RL = "RDFRank + Level: right to left";
    public static final String GLOBALSTRATEGY_RDFRANK = "RDFRank";
    public static final String GLOBALSTRATEGY_RDF_LEVEL_LR = "RDFRank + Level: left to right";
    public static final String GLOBALSTRATEGY_RDF_LEVEL_RL = "RDFRank + Level: right to left";

    public static final String COMPACT_GRAPH = "Compact graph";
	public static final int CONTAINER_SIZE = 65;

    public static final String DEFAULT_LANGUAGE = "en";
	
	final static int WIDTH_MARGIN = 30;
    final static int HEIGHT_MARGIN = 30;
    final static int BUTTON_SIZE = 10;
	public final static String SIDCLASS = "SIDClass_";
    public final static int NEEDED_HEIGHT = 245; // TODO
    public final static String THING_ENTITY = "http://www.w3.org/2002/07/owl#Thing";
	public final static String NOTHING_ENTITY = "http://www.w3.org/2002/07/owl#Nothing";
	
	/**
	 * Curve drawing method
	 */
	final static int BEZIER = 1;
	final static int NURB   = 2;
}
