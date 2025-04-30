package sid.OntView2.kcExtractors;

import it.essepuntato.semanticweb.kce.engine.Engine;
import it.essepuntato.taxonomy.HTaxonomy;
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisConstants;
import sid.OntView2.utils.OWLAPITaxonomyMakerExtended;

import java.util.Map;
import java.util.Set;

public class KCEConceptExtraction extends KConceptExtractor {

	/**
	 * Replaced by overloaded version
	 * Retrieves a set of Key Concepts to be shown by using KCE Api
	 */
	@Override
	public Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap, int limitResultSize) {
		boolean considerImportedOntologies = true; /* True if you want t*/

		if (activeOntology != null) {
			HTaxonomy ht = new OWLAPITaxonomyMakerExtended(activeOntology, considerImportedOntologies).makeTaxonomy();
			Engine e = new Engine(ht);
			e.setNumberOfKeyConceptsToExtract(limitResultSize);
			e.run();
			Set<String> conceptSet = e.getKeyConcepts();
			conceptSet.add(VisConstants.THING_ENTITY);
			conceptSet.add(VisConstants.NOTHING_ENTITY); 
			return conceptSet;

		} else {
			System.err.println("hideNonKeyConcepts is WIP in protege version. Check source string");
			return null;
		}
	}
}

