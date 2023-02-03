package update.ottr;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
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

    /**
     * Adds the inner sub query to the builder.
     * This finds all graphs that match the deleteModel and have a blank node.
     */
    private void addInnerSubQuery(SelectBuilder builder, Model model) {
        addWhereClause(builder, model);
        try {
            builder.addFilter("isblank(?blank)");
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Adds the outer sub query to the builder.
     * This counts the number of triples the the sub query has.
     */
    private void addOuterSubQuery(SelectBuilder builder, Model model) {
        // TODO: add the count as a function
        builder.addVar("sub").addVar("count");
        builder.addWhere("?sub", "?pred", "?obj");
    }

    public UpdateRequest createDelRequest(Model deleteModel) {
        int count = countBlankNodes(deleteModel);
        log.print(LOGTAG.DEBUG, "" + count);

        // null pointer if we dont init this
        org.apache.jena.query.ARQ.init();

        // create the outer query
        SelectBuilder builder = new SelectBuilder();
        builder.addVar("blank").addVar("count");
        addWhereClause(builder, deleteModel);
        builder.setLimit(1);
        builder.addGroupBy("?blank");
        try {
            builder.addHaving("?count > " + count);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // create the outer sub query
        SelectBuilder outerSubBuilder = new SelectBuilder();
        addOuterSubQuery(outerSubBuilder, deleteModel);

        // create the inner sub query
        SelectBuilder innerSubBuilder = new SelectBuilder();
        innerSubBuilder.addVar("?blank");
        addInnerSubQuery(innerSubBuilder, deleteModel);

        // set sub queries
        outerSubBuilder.addSubQuery(innerSubBuilder);
        builder.addSubQuery(outerSubBuilder);

        log.print(logLevel, "top level:\n" + builder.buildString());

        return null;
    }

    // run blanknode update
}
