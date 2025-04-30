package sid.OntView2.utils;
import java.io.File;
import java.util.HashSet;
import java.util.Optional;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class Util {
	static final String freshClassPrefix = "#Class_";
	static int nextFreshId = 0;

	static public OWLOntology load(String iriString, boolean local, OWLOntologyManager manager)
			throws OWLOntologyCreationException {
		OWLOntology onto;

		if(!local){
			IRI iri = IRI.create(iriString);
			onto = manager.loadOntologyFromOntologyDocument(iri);
		}
		else {
			File file = new File(iriString);
			onto = manager.loadOntologyFromOntologyDocument(file);
		}
		return onto;
	}

    static public void save(OWLOntology onto, String destFile, OWLOntologyManager manager)
			throws OWLOntologyStorageException {
        File file = new File(destFile);
		manager.saveOntology(onto, IRI.create(file.toURI()));
	}

    static public OWLClass createFreshClass(OWLOntology onto, OWLDataFactory dataFactory, OWLOntologyManager manager){
        Optional<IRI> optionalIRI = onto.getOntologyID().getOntologyIRI();
        if (optionalIRI.isPresent()) {
            OWLClass cls = dataFactory.getOWLClass(
                freshClassPrefix + nextFreshId,
                new DefaultPrefixManager(optionalIRI.get().toString())
            );
            OWLDeclarationAxiom declAxiom = dataFactory.getOWLDeclarationAxiom(cls, new HashSet<>());
            manager.addAxiom(onto, declAxiom);
            ++nextFreshId;
            return cls;
        }
        else {
            throw new IllegalStateException("Ontology does not have an IRI");
        }
	}
}
