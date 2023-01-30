package update.ottr;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.update.UpdateRequest;

// This solution is about removing the local blank node assumption. 
public class BlankNode {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.DEFAULT;

    public BlankNode(Logger log) {
        this.log = log;
    }

    /**
     * Count the number of triples containing one or more blank nodes in the model.
     **/
    private int countBlankNodes(Model model) {
        int count = 0;
        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            // if the statement contains a blank node
            if (statement.getSubject().isAnon() || statement.getObject().isAnon()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adds all triples in the deleteModel to the where clause of the builder.
     * If a triple contains a blank node, it is added as a variable.
     **/
    private void addWhereClause(SelectBuilder builder, Model model) {
        StmtIterator statements = model.listStatements();
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
    }

    public UpdateRequest createDelRequest(Model deleteModel) {
        int count = countBlankNodes(deleteModel);
        log.print(LOGTAG.DEBUG, "" + count);

        SelectBuilder builder = new SelectBuilder();
        builder.addVar("blank").addVar("count");
        addWhereClause(builder, deleteModel);

        SelectBuilder innerSubBuilder = new SelectBuilder();
        innerSubBuilder.addVar("?blank");
        addWhereClause(innerSubBuilder, deleteModel);

        Expr e = new ExprVar("IsBlank(?blank)");

        innerSubBuilder.addFilter(e);

        log.print(logLevel, innerSubBuilder.buildString());

        // log.print(LOGTAG.DEBUG, "\n" + builder.build());
        // return request;
        return null;
    }

    // run blanknode update
}
