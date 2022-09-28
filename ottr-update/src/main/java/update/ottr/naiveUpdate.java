package update.ottr;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class naiveUpdate {
    private static String updateQuery = "";

    public static String createDeleteQuery(Model oldModel) {
        
        updateQuery = "DELETE WHERE {";

        StmtIterator oldIterator = oldModel.listStatements();
        for ( ; oldIterator.hasNext() ; ) {
            Statement stmt = oldIterator.nextStatement();
            String deleteTriple = "\n";
            deleteTriple += stmt.getSubject().toString() + " ";
            deleteTriple += stmt.getPredicate().toString() + " ";
            deleteTriple += stmt.getObject().toString() + " .";
            updateQuery += "\n" + deleteTriple;
        }

        updateQuery += "\n}";
        return updateQuery;
    }

    public static String createInsertQuery(Model newModel) {
        
        updateQuery = "INSERT WHERE {";

        StmtIterator newIterator = newModel.listStatements();
        for ( ; newIterator.hasNext() ; ) {
            Statement stmt = newIterator.nextStatement();
            String insertTriple = "\n";
            insertTriple += stmt.getSubject().toString() + " ";
            insertTriple += stmt.getPredicate().toString() + " ";
            insertTriple += stmt.getObject().toString() + " .";
            updateQuery += "\n" + insertTriple;
        }

        updateQuery += "\n}";
        return updateQuery;
    }

    public static String createUpdateQuery(Model oldModel, Model newModel) {
        updateQuery = "DELETE {";
        // TODO: add delete clause
        updateQuery += "\n}";
        updateQuery += "INSERT {";
        //TODO: add insert clause
        updateQuery += "\n}";
        updateQuery += "WHERE {";
        //TODO: add where clause
        updateQuery += "\n}";
        return updateQuery;
    }
}
