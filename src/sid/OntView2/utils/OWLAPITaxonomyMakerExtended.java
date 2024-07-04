package sid.OntView2.utils;

import it.essepuntato.facility.list.ListFacility;
import it.essepuntato.semanticweb.owlapi.OWLAPIManager;
import it.essepuntato.taxonomy.Category;
import it.essepuntato.taxonomy.HTaxonomy;
import it.essepuntato.taxonomy.Instance;
import it.essepuntato.taxonomy.Property;
import it.essepuntato.taxonomy.exceptions.NoCategoryException;
import it.essepuntato.taxonomy.exceptions.NoInstanceException;
import it.essepuntato.taxonomy.exceptions.NoPropertyException;
import it.essepuntato.taxonomy.exceptions.RootException;
import it.essepuntato.taxonomy.maker.ITaxonomyMaker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.InputStream;
import java.util.*;


/**
 * A generator of a simple taxonomy for any ontology that happens to be readable by OWL API 3.0
 * the default one for NeOn Toolkit 2.3. It contains methods simply going
 * through the ontology in question and deriving all its sub-classes, instances, etc.
 * 
 * This taxonomy is then used as an input to calculating the vector of N key concepts...
 * (It is an equivalent for WatsonTaxonomyMaker defined in Silvio's it.essepuntato.ontoalg.)
 * 
 * @author Ning Li, KMi
 * 
 * 
 */

 /**
  * Modification log : Copied the whole class in order to add a constructor with OwlOntology as parameter
  * @author bob
  *
  */

public class OWLAPITaxonomyMakerExtended implements ITaxonomyMaker{
		private Number nSize = null;
		private OWLAPIManager owlAPIManager = null;
		private OWLOntology ontology = null;
		
		public final static String rootName = "http://www.essepuntato.it/OntoAlgorithm#ESSEPUNTATO";

		/**
		 * Constructor, initializes OWL Model handler based on the supplied ontology and project names
		 * @throws OWLOntologyCreationException
		 */
				
	   public OWLAPITaxonomyMakerExtended(String uri, boolean useImports){
			
		   OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); 
			try {
				ontology = manager.loadOntology(IRI.create(uri));
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 owlAPIManager = new OWLAPIManager(ontology, useImports);
		}
	   
	   public OWLAPITaxonomyMakerExtended(OWLOntology activeOntology, boolean useImports){
			
		   ontology = activeOntology;
		   owlAPIManager = new OWLAPIManager(ontology, useImports);
		}
		
	   public OWLAPITaxonomyMakerExtended(InputStream uri, boolean useImports){
			
		   OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); 
			try {
				ontology = manager.loadOntologyFromOntologyDocument(uri);
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			owlAPIManager = new OWLAPIManager(ontology, useImports);
		}
	   
	   public OWLAPITaxonomyMakerExtended(StringDocumentSource content, boolean useImports){
			
		   OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); 
			try {
				ontology = manager.loadOntologyFromOntologyDocument(content);
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 owlAPIManager = new OWLAPIManager(ontology, useImports);
		}
	   
		public String getDefaultTaxonomyRoot() {
			return rootName;
		}
		
		public Number getTaxonomySize() {
			return nSize;
		}
		
		public OWLOntology getOWLOntology() {
			return ontology;
		}

		
		
		public HTaxonomy makeTaxonomy() {
			System.out.println("Finding all the classes of the ontology (looking for the included ontologies):");
			Set<OWLClass> classesBase = owlAPIManager.getAllClasses();
				
			Set<OWLClass> classes = this.cleanClassList(classesBase);

			System.out.println("Total classes found: " + classes.size());

			System.out.print("Finding all the subclasses for all these classes... ");
			Hashtable<String, ArrayList<String>> subClasses = this.categorizeAllSubclasses(this.owlAPIManager, classes);
			System.out.println("done");

			System.out.println("Finding all the root classes of the taxonomy:");
			ArrayList<String> rootClasses = this.getRootClasses(this.owlAPIManager, subClasses);

			boolean isOneRoot = (rootClasses.size() == 1 ? true : false);

			/* I make the taxonomy */
			HTaxonomy t = new HTaxonomy();
			Category root = null;
			if (isOneRoot) {
				root = new Category(rootClasses.get(0));
			}
			else {
				root = new Category(OWLAPITaxonomyMakerExtended.rootName);
			}
			try {
				t.addCategory(root);
				t.setRoot(root);
			} catch (NoCategoryException e) {
				System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
						root.getName() + "' isn't in the taxonomy");
				e.printStackTrace();
			} catch (RootException e) { /* This exception is almost impossible */
				System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
						root.getName() + "' cannot be a root category");
				e.printStackTrace();
			}

			/* I add all the categories acquired from the ontology to the constructed taxonomy 
			 * using 'flat' structure first */
			Iterator<OWLClass> classesIterator = classes.iterator();
			while (classesIterator.hasNext()) {
				t.addCategory(new Category(classesIterator.next().getIRI().toString()));
			}

			/* If there are more than one root category, I add the proper relations from
			 * my 'faked root' category to all the 'real root' categories */
			if (!isOneRoot) {
				Iterator<String> rootClassesIterator = rootClasses.iterator();
				while (rootClassesIterator.hasNext()) {
					String name = rootClassesIterator.next();
					try {
						Category category = t.getCategoryByName(name);
						t.subCategoryOf(category, t.getRoot());
					} catch (NoCategoryException ex) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
								name + "' isn't in the taxonomy");
						ex.printStackTrace();
					} catch (RootException ex) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
								name + "' isn't the root");
						ex.printStackTrace();
					}
				}
			}

			/* Now, starting from each 'real root' category (at least one), we can generate 
			 * all the proper relations for the taxonomy */
			Iterator<String> rootClassesIterator = rootClasses.iterator();
			while (rootClassesIterator.hasNext()) {
				String rootClass = rootClassesIterator.next();
				try {
					Category curTopCat = t.getCategoryByName(rootClass);
					t = this.makeTaxonomyStartingFrom(
							t, this.owlAPIManager, subClasses, curTopCat, new ArrayList<Category>());
				} catch (NoCategoryException e) {
					System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
							rootClass + "' isn't in the taxonomy");
					e.printStackTrace();
				}
			}

			this.nSize = new Integer(t.getAllCategories().size());
			
			/* Look for properties */
			for (OWLClass curClass : classes) {
				for (OWLObjectProperty objectProperty : 
					owlAPIManager.getAllObjectPropertyHavingTheClassAsDomain(curClass)) {
					Property property = new Property(objectProperty.toStringID());
					t.addProperty(property);
					try {
						t.setDomain(property, t.getCategoryByName(curClass.getIRI().toString()));
					} catch (NoCategoryException e) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
								curClass.getIRI().toString() + "' isn't in the taxonomy");
						e.printStackTrace();
					} catch (NoPropertyException e) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The property '" + 
								property.getName() + "' isn't in the taxonomy");
						e.printStackTrace();
					}
				}
				
				for (OWLDataProperty dataProperty : 
					owlAPIManager.getAllDataPropertyHavingTheClassAsDomain(curClass)) {
					Property property = new Property(dataProperty.toStringID());
					t.addProperty(property);
					try {
						t.setDomain(property, t.getCategoryByName(curClass.getIRI().toString()));
					} catch (NoCategoryException e) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The category '" + 
								curClass.getIRI().toString() + "' isn't in the taxonomy");
						e.printStackTrace();
					} catch (NoPropertyException e) {
						System.err.println("[TaxonomyMaker: makeTaxonomy] ERROR - The property '" + 
								property.getName() + "' isn't in the taxonomy");
						e.printStackTrace();
					}
				}
			}
			
			return t;
		}
		
		private Set<OWLClass> cleanClassList(Set<OWLClass> classes) {
			Iterator<OWLClass> classesIterator = classes.iterator();
			Set<OWLClass> result = new HashSet<OWLClass>();
			while (classesIterator.hasNext()) {
				OWLClass curClass = classesIterator.next();
				String curClassStr= curClass.getIRI().toString();
				if (!curClassStr.contains("bnode") &&
						!curClassStr.contains("http://www.w3.org/2002/07/owl#Class") &&
						!curClassStr.contains("http://www.w3.org/2002/07/owl#Thing") &&
						!curClassStr.contains("http://www.w3.org/2002/07/owl#ObjectProperty") &&
						!curClassStr.contains("http://www.w3.org/2002/07/owl#FunctionalProperty") &&
						!curClassStr.contains("http://www.w3.org/2002/07/owl#DataProperty"))
					result.add(curClass);
			}
			
			return result;
		}
		
		/**
		 * Goes through the acquired list of classes in the ontology and categorize them into 
	     * aClass -> (aSubClass1, aSubClass2,...) 
		 * @param owlMgr
		 * @param classes
		 * @return
		 */
		private Hashtable<String, ArrayList<String>> categorizeAllSubclasses(
				OWLAPIManager owlMgr, 
				Set<OWLClass> classes) {
			
			Hashtable<String, ArrayList<String>> result = new Hashtable<String, ArrayList<String>>();

			/* I initialize the Hashtable */
			Iterator<OWLClass> classesIterator = classes.iterator();
			while (classesIterator.hasNext()) {
				result.put(classesIterator.next().getIRI().toString(), new ArrayList<String>());
			}

			/* I find the subclasses */
			classesIterator = classes.iterator();
			while (classesIterator.hasNext()) 
			{				
				OWLClass aClass = classesIterator.next();
				Set<OWLClass> subClassesDirect = owlMgr.getAllSubClasses(aClass);
				if (subClassesDirect != null) 
				{
					for (OWLClass aSubClass : subClassesDirect) 
					{
				 	    String aSubClassUri = aSubClass.getIRI().toString();
						List<String> subClassList = result.get(aClass.getIRI().toString());
						if (!ListFacility.containsTheSameStringValue(subClassList, aSubClassUri))
                        {
							subClassList.add(aSubClassUri);
						}
					}
				}
			
				
			}

			return result;
		}
		/**
		 * Identifies root classes in the list = classes with no named parent class
		 * @param owlMgr
		 * @param allSubClasses
		 * @return
		 */
		private ArrayList<String> getRootClasses(
				OWLAPIManager owlMgr, Hashtable<String, ArrayList<String>> allSubClasses) {
			ArrayList<String> result = new ArrayList<String>();

			Iterator<String> classesIterator = allSubClasses.keySet().iterator();
			
			while (classesIterator.hasNext()) {
				String aClass = classesIterator.next();
				Iterator<ArrayList<String>> subclassesIterator = allSubClasses.values().iterator();

				boolean isInSubclasses = false;
				while (subclassesIterator.hasNext() && !isInSubclasses) {
					ArrayList<String> curSubclasses = subclassesIterator.next();
					isInSubclasses = curSubclasses.contains(aClass);
				}

				if (!isInSubclasses) {
					System.out.println("'" + aClass + "' is a root category");
					result.add(aClass);
				}
			}

			return result;
		}
		/**
		 * Creates taxonomic relations between current top and its instances / sub-classes
		 * and goes recursively to do the same for sub-classes found here
		 * @param t
		 * @param owlMgr
		 * @param subClasses
		 * @param curTopCategory
		 * @param alreadyProcessed
		 * @return
		 */
		private HTaxonomy makeTaxonomyStartingFrom(
				HTaxonomy t, 
				OWLAPIManager owlMgr, 
				Hashtable<String, ArrayList<String>> subClasses, 
				Category curTopCategory,
				List<Category> alreadyProcessed) {
			
			if (!ListFacility.containsTheSameObject(alreadyProcessed, curTopCategory)) {
				
					OWLClass id = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLClass(
							IRI.create(curTopCategory.getName()));
					
					Set<OWLIndividual> instances = owlMgr.getDirectIndividuals(id);
					for (OWLIndividual ind : instances) {
						Instance instance = new Instance(ind.toStringID()); 
						t.addInstance(instance);
						try {
							t.instanceOf(instance, curTopCategory);
						} catch (NoInstanceException e) {
							System.err.println("[TaxonomyMaker: makeTaxonomyStartingFrom] ERROR - The instance '" + 
									instance.getName() + "' isn't in the taxonomy");
							e.printStackTrace();
						} catch (NoCategoryException e) {
							System.err.println("[TaxonomyMaker: makeTaxonomyStartingFrom] ERROR - The category '" + 
									curTopCategory.getName() + "' isn't in the taxonomy");
							e.printStackTrace();
						}
					}
					
					/*  we add all the subclasses referring to the given category */
					// start by getting the list of subclasses associated with a given category ID
					Iterator<String> iteSubclasses = subClasses.get(curTopCategory.getName()).iterator();
					while (iteSubclasses.hasNext()) {
						try {
							String curSubClasses = iteSubclasses.next();
							Category curSubCategory = t.getCategoryByName(curSubClasses);
							// create the actual link for the taxonomic purposes
							t.subCategoryOf(curSubCategory, curTopCategory);
							alreadyProcessed.add(curTopCategory);
							// go recursively for each subclass of the current category (if non-empty)
							this.makeTaxonomyStartingFrom(t, owlMgr, subClasses, curSubCategory, alreadyProcessed);

						} 
						catch (NoCategoryException ex) {
							System.err.println("[TaxonomyMaker: makeTaxonomyStartingFrom] ERROR - The category '" +
									curTopCategory.getName() + "' isn't in the taxonomy");
							ex.printStackTrace();
						} catch (RootException ex) {
							System.err.println("[TaxonomyMaker: makeTaxonomyStartingFrom] ERROR - The category '" +
									curTopCategory.getName() + "' isn't the root");
							ex.printStackTrace();
						} 
					}
					
					
			}

			return t;
		}
		
}
