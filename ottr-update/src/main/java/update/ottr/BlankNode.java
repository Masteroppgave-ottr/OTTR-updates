package update.ottr;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;

// This solution is about removing the local blank node assumption. 
public class BlankNode {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.DEFAULT;

    public BlankNode(Logger log) {
        this.log = log;
    }

    public UpdateRequest createDelRequest(Model deleteModel) {
        // find all occurences of blank nodes in the old model

        int count = 0; // has to be several when considering multiple blank nodes

        // count the number of occurences of the blank node in the old model
        log.print(LOGTAG.DEBUG, "lez go!");

        StmtIterator statements = deleteModel.listStatements();
        while (statements.hasNext()) {
            // if the statement contains a blank node
            Statement statement = statements.next();
            if (statement.getSubject().isAnon() || statement.getObject().isAnon()) {
                // increment count
                count++;
            }
        }

        // UpdateBuilder builder = new UpdateBuilder().;
        SelectBuilder builder = new SelectBuilder();
        builder.addVar("blank").addVar("count");
        statements = deleteModel.listStatements();
        while (statements.hasNext()) {
            // if the statement contains a blank node
            Statement statement = statements.next();
            log.print(LOGTAG.DEBUG, statement.toString());

            String sub = null;
            String obj = null;
            if (statement.getSubject().isAnon()) {
                sub = "?" + statement.getSubject().toString();
            }

            if (statement.getObject().isAnon()) {
                obj = "?" + statement.getObject().toString();
            }

            if (sub != null && obj != null) {
                builder.addWhere(sub, statement.getPredicate(), obj);
            } else if (sub != null) {
                builder.addWhere(sub, statement.getPredicate(), statement.getObject());
            } else if (obj != null) {
                builder.addWhere(statement.getSubject(), statement.getPredicate(), obj);
            } else {
                builder.addWhere(statement.getSubject(), statement.getPredicate(), statement.getObject());
            }
        }

        log.print(LOGTAG.DEBUG, "\n" + builder.build());
        // return request;
        return null;
    }

    // run blanknode update
}
