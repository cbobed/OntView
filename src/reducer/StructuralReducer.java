package reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.parameters.Imports;

import sid.OntView2.utils.Params;
import sid.OntView2.utils.Util;

public class StructuralReducer {
    private static final Logger logger = LogManager.getLogger(StructuralReducer.class);

    public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
	public static Map<OWLClassExpression, OWLClass> map = new HashMap<OWLClassExpression, OWLClass>();
	public static Stack<OWLClassExpression> stack = new Stack<OWLClassExpression>();
	public static Set<OWLAxiom> toDelete = new HashSet<OWLAxiom>();
	public static Set<OWLAxiom> toAdd = new HashSet<OWLAxiom>();
	public static OWLOntology onto = null;
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		String ontoFile = "C:\\Users\\bob\\Downloads\\univ-bench.owl.xml";
		boolean direct = true;
		
		if(Params.verbose)
            logger.debug("Processing {}", ontoFile);
		
		OWLOntology onto = Util.load(ontoFile, direct, manager);
		if(!Params.forComparison)
			Util.save(onto, ontoFile.replace("/", "/reduced/"), manager);
		
		long start = System.nanoTime();
		applyStructuralReduction(onto);
        logger.debug("Took {}ms", (System.nanoTime() - start) / 1000000);
		Util.save(onto, ontoFile.replace("/", "/reduced/").replace(".","_red."), manager);
	}
	
	public static void applyStructuralReduction(OWLOntology onto){
		StructuralReducer.onto = onto;
		OWLAxiomReducerVisitor axVisitor = new OWLAxiomReducerVisitor();
		
		for (OWLAxiom ax : onto.getTBoxAxioms(Imports.EXCLUDED)) {
            logger.debug("\n{}", ax);
			ax.accept(axVisitor);									
		}

        logger.debug("{}/{}", toAdd.size(), onto.getTBoxAxioms(Imports.EXCLUDED).size());
		
		OWLClassExpression ce = null;
		OWLClassExpressionVisitor visitor = new OWLClassExpressionReducerVisitor();
		int b = 0;
        logger.debug("Initial stack {}", stack.size());
		
		while(!stack.isEmpty()){
			ce = stack.pop();
			ce.accept(visitor);
			OWLClass newClass = map.containsKey(ce) ? map.get(ce) : Util.createFreshClass(onto, dataFactory, manager);
			
			toAdd.add(dataFactory.getOWLEquivalentClassesAxiom(newClass, ce));
			++b;
		}
        logger.debug(b);
		
		manager.removeAxioms(onto, toDelete);
		manager.addAxioms(onto, toAdd);
		
	}
	
	/**
	 * Modified version for OntView needs
	 * @param onto
	 */
	public static void customApplyStructuralReduction(OWLOntology onto,Map<String,? extends Object> extMap){
		StructuralReducer.onto = onto;
		OWLAxiomReducerVisitor axVisitor = new OWLAxiomReducerVisitor();
		
		for (OWLAxiom ax : onto.getTBoxAxioms(Imports.EXCLUDED)) {
			ax.accept(axVisitor);									
		}
		
		OWLClassExpression ce = null;
		OWLClassExpressionVisitor visitor = new OWLClassExpressionReducerVisitor();
		int b = 0;
		
		while(!stack.isEmpty()){
			ce = stack.pop();
			if (extMap.get(ce.toString()) != null) {
				continue;
			}	 
//			ce.accept(visitor);
			OWLClass newClass = map.containsKey(ce) ? map.get(ce) : Util.createFreshClass(onto, dataFactory, manager);
			
			toAdd.add(dataFactory.getOWLEquivalentClassesAxiom(newClass, ce));
			++b;
		}
		
		manager.removeAxioms(onto, toDelete);
		manager.addAxioms(onto, toAdd);
		
	}
}
