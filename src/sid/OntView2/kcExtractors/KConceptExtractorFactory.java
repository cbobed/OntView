package sid.OntView2.kcExtractors;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisConstants;

import java.util.Map;

public class KConceptExtractorFactory {
	public static KConceptExtractor getInstance(String id, Map<String, Shape> shapeMap) {
		KConceptExtractor result = null; 
		switch (id) {
			case VisConstants.KCECOMBOOPTION1, VisConstants.KCECOMBOOPTION2, VisConstants.KCECOMBOOPTION3
										-> {result = new KCEConceptExtraction();}
			case VisConstants.PAGERANKCOMBOOPTION1, VisConstants.PAGERANKCOMBOOPTION2, VisConstants.PAGERANKCOMBOOPTION3
										-> {result = new PageRankConceptExtraction(); }
			case VisConstants.RDFRANKCOMBOOPTION1, VisConstants.RDFRANKCOMBOOPTION2, VisConstants.RDFRANKCOMBOOPTION3
										-> {result = new RDFRankConceptExtraction(); }
            case VisConstants.CUSTOMCOMBOOPTION -> {result = new CustomConceptExtraction(shapeMap); }
		}
		return result; 
	}
}
