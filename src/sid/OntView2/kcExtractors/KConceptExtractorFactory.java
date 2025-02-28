package sid.OntView2.kcExtractors;

import sid.OntView2.common.VisConstants;

public class KConceptExtractorFactory {
	public static KConceptExtractor getInstance(String id) {
		KConceptExtractor result = null; 
		switch (id) {
			case VisConstants.KCECOMBOOPTION1, VisConstants.KCECOMBOOPTION2 
										-> {result = new KCEConceptExtraction();}
			case VisConstants.PAGERANKCOMBOOPTION1, VisConstants.PAGERANKCOMBOOPTION2 
										-> {result = new PageRankConceptExtraction(); }
			case VisConstants.RDFRANKCOMBOOPTION1, VisConstants.RDFRANKCOMBOOPTION2 
										-> {result = new RDFRankConceptExtraction(); }
			case VisConstants.NONECOMBOOPTION 
										-> {result = null; }
		}
		return result; 
	}
}
