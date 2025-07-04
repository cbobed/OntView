package sid.OntView2.utils;

import com.ctc.wstx.exc.WstxParsingException;
import org.apache.axiom.om.OMException;
import org.semanticweb.HermiT.datatypes.MalformedLiteralException;
import org.semanticweb.HermiT.datatypes.UnsupportedDatatypeException;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;

public class ErrorHandler {

    /**
     * Returns a user-friendly message for errors occurring during ontology loading.
     */
    public static String getOntologyLoadError(Throwable ex) {
        Throwable cause = unwrapCause(ex);

        if (cause instanceof ConnectException) {
            return "Error: Could not connect to the imported ontology’s server.";
        }
        if (cause instanceof OWLOntologyCreationIOException) {
            return "Error: I/O error while loading the ontology (network or file issue).";
        }
        if (cause instanceof UnloadableImportException) {
            return "Error: Failed to load an imported ontology.";
        }
        if (cause instanceof UnknownHostException) {
            return "Error: No Internet connection.";
        }
        if (cause instanceof FileNotFoundException) {
            return "Error: The ontology file was not found.";
        }
        if (cause instanceof OWLOntologyCreationException) {
            return "Error: The ontology is corrupted or invalid.";
        }

        return truncateMessage(cause);
    }

    /**
     * Returns a user-friendly message for errors occurring during reasoner loading or inference.
     */
    public static String getReasonerError(Throwable ex) {
        Throwable cause = unwrapCause(ex);

        if (cause instanceof UnsupportedDatatypeException) {
            return "The ontology uses a datatype that the reasoner cannot handle.";
        }
        if (cause instanceof MalformedLiteralException) {
            return "Error: Malformed literal in the ontology.";
        }
        if (cause instanceof OMException) {
            return "Error: Malformed XML structure.";
        }
        if (cause instanceof WstxParsingException) {
            return "Error: XML parsing issue due to undeclared namespace or invalid syntax.";
        }
        if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
            return "Error: Required reasoner libraries are missing.";
        }
        if (cause instanceof LinkageError) {
            return "Error: OWL-API version is incompatible with the reasoner.";
        }
        if (cause instanceof OutOfMemoryError) {
            return "Error: Insufficient memory to run this operation.";
        }
        if (cause instanceof ConnectException) {
            return "Error: Could not connect to the reasoner server.";
        }

        return truncateMessage(cause);
    }

    /**
     * Returns a user-friendly message for errors occurring during load and reasoning tasks.
     */
    public static String getLoadAndReasonError(Throwable ex) {
        Throwable cause = unwrapCause(ex);

        if (cause instanceof CancellationException) {
            return "Error: Operation cancelled by user.";
        }
        if (cause instanceof InterruptedException) {
            return "Error: Operation was interrupted.";
        }
        return getReasonerError(ex);
    }

    /**
     * Returns a user-friendly message for errors occurring during graph state XML saving.
     */
    public static String getGraphSaveError(Throwable ex) {
        Throwable cause = unwrapCause(ex);

        if (cause instanceof IOException) {
            return "Error: I/O error while saving graph state to XML (disk or file issue).";
        }
        if (cause instanceof ParserConfigurationException || cause instanceof TransformerException) {
            return "Error: XML processing error (configuration or transformation issue).";
        }
        if (cause instanceof JAXBException) {
            return "Error: XML marshalling/unmarshalling failure.";
        }
        if (cause instanceof SecurityException) {
            return "Error: Insufficient permissions to write the XML file.";
        }
        if (cause instanceof InterruptedException) {
            return "Error: Operation was interrupted while saving the graph state.";
        }

        return truncateMessage(cause);
    }

    /**
     * Returns a user-friendly message for errors occurring during graph state XML saving.
     */
    public static String getXMLeError(Throwable ex) {
        Throwable cause = unwrapCause(ex);

        if (cause instanceof SAXParseException sax) {
            return String.format("Error: Malformed XML at line %d, column %d: %s",
                sax.getLineNumber(), sax.getColumnNumber(), sax.getMessage());
        }
        if (cause instanceof IOException) {
            return "Error: I/O issue while reading the XML for restore view.";
        }
        if (cause instanceof ParserConfigurationException) {
            return "Error: XML parser configuration invalid.";
        }
        return truncateMessage(cause);
    }

    /**
     * Unwraps nested causes to find the root cause.
     */
    private static Throwable unwrapCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Truncates a message if too long
     */
    private static String truncateMessage(Throwable cause) {
        String msg = cause.getMessage();
        if (msg != null && msg.length() > 120) {
            msg = msg.substring(0, 120) + " …";
        }

        return "Error: " + (msg != null ? msg + "." : "unknown.");
    }
}
