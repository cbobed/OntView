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
    public static final String PARTIALSTRATEGY_RDFRANK = "RDFRankPartial";
    public static final String PARTIALSTRATEGY_RDF_LEVEL = "RDFRankLevelPartial";

    public static final String FULLSTRATEGY_RDFRANK = "RDFRank";
    public static final String FULLSTRATEGY_RDF_LEVEL = "RDFLevel";

	public static final int CONNECTOROFFSET = 7;

	public static final int CONTAINER_SIZE = 65;
	
	final static int WIDTH_MARGIN        = 30;
    final static int HEIGHT_MARGIN       = 30;
	final static int PROGRESSBAROBSERVER = 1;
	final static int GENERALOBSERVER     = 2;
    final static String THING_ENTITY = "http://www.w3.org/2002/07/owl#Thing";
	final static String NOTHING_ENTITY = "http://www.w3.org/2002/07/owl#Nothing";
	public final static String SIDCLASS = "SIDClass_";
    public final static int NEEDED_HEIGHT = 245; // TODO


	final int KEYCONCEPTNO = 10;
	
	
	
	/**
	 * Curve drawing method
	 */
	final static int BEZIER = 1;
	final static int NURB   = 2;
}
