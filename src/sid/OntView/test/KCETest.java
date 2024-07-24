
package sid.OntView.test;

import java.util.Set;

import it.essepuntato.semanticweb.kce.engine.Engine;
import it.essepuntato.taxonomy.HTaxonomy;
import it.essepuntato.taxonomy.maker.OWLAPITaxonomyMaker;


public class KCETest {
	
	public static void main (String args[]) {
		 boolean considerImportedOntologies = true; /* True if you want t*/
		 HTaxonomy ht = new OWLAPITaxonomyMaker("file:"+args[0], considerImportedOntologies).makeTaxonomy();
		 Engine e = new Engine(ht);
		 e .setNumberOfKeyConceptsToExtract(Integer.valueOf(args[1]));
		 e.run();
		 Set<String> conceptSet = e.getKeyConcepts();
		 
		 for (String cpt: conceptSet) {
			 System.out.println(cpt); 
		 }
	}
	    
}
