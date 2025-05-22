package sid.OntView2.utils;

import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnloadableImportException;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.UnknownHostException;

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

        if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
            return "Error: Required reasoner libraries are missing (check your classpath).";
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
        if (msg != null && msg.length() > 100) {
            msg = msg.substring(0, 100) + "…";
        }
        return "Error: " + (msg != null ? msg + "." : "unknown.");
    }
}
