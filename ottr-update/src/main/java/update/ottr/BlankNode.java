package update.ottr;

import java.io.FileNotFoundException;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

// This solution is about removing the local blank node assumption. 
public class BlankNode {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.BLANK;

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
                sub = "?" + statement.getSubject().toString().replace("-", "_");
            }

            if (statement.getObject().isAnon()) {
                obj = "?" + statement.getObject().toString().replace("-", "_");
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
     * Adds all triples in the deleteModel to the where clause of the builder.
     * If a triple contains a blank node, it is added as a variable.
     **/
    private String addWhereClause2(UpdateBuilder builder, Model model) {
        StmtIterator statements = model.listStatements();
        String lastBlank = null;
        while (statements.hasNext()) {
            // if the statement contains a blank node
            Statement statement = statements.next();
            log.print(LOGTAG.DEBUG, statement.toString());

            String sub = null;
            String obj = null;
            if (statement.getSubject().isAnon()) {
                sub = "?" + statement.getSubject().toString().replace("-", "_");
            }

            if (statement.getObject().isAnon()) {
                obj = "?" + statement.getObject().toString().replace("-", "_");
            }

            if (sub != null && obj != null) {
                builder.addDelete(sub, statement.getPredicate(), obj);
                // TODO: handle this case later
            } else if (sub != null) {
                builder.addDelete(sub, statement.getPredicate(), statement.getObject());
                lastBlank = sub;
            } else if (obj != null) {
                builder.addDelete(statement.getSubject(), statement.getPredicate(), obj);
                lastBlank = obj;
            } else {
                builder.addDelete(statement.getSubject(), statement.getPredicate(), statement.getObject());
            }
        }
        return lastBlank;
    }

    /**
     * Adds the inner sub query to the builder.
     * This finds all graphs that match the deleteModel and have a blank node.
     */
    private void addInnerSubQuery(SelectBuilder builder, Model model, String blankName) {
        addWhereClause(builder, model);
        try {
            builder.addFilter("isblank(" + blankName + ")");
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Adds the outer sub query to the builder.
     * This counts the number of triples the the sub query has.
     */
    private void addOuterSubQuery(SelectBuilder builder, Model model, int count, String blankName) {
        builder.addVar(blankName);
        try {
            builder.addVar("count(" + blankName + ")", "count");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        builder.addWhere(blankName, "?pred", "?obj");
        SelectBuilder newBuilder = new SelectBuilder().addWhere("?obj", "?pred", blankName);
        builder.addUnion(newBuilder);

        builder.addGroupBy(blankName);
        try {
            builder.addHaving("?count = " + count);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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

        // create the outer sub query
        SelectBuilder outerSubBuilder = new SelectBuilder();
        addOuterSubQuery(outerSubBuilder, deleteModel, count, "?blank");

        // create the inner sub query
        SelectBuilder innerSubBuilder = new SelectBuilder();
        innerSubBuilder.addVar("?blank");
        addInnerSubQuery(innerSubBuilder, deleteModel, "?blank");

        // set sub queries
        outerSubBuilder.addSubQuery(innerSubBuilder);
        builder.addSubQuery(outerSubBuilder);

        log.print(logLevel, "top level:\n" + builder.buildString());

        return null;
    }

    public void createDeleteRequest(Model deleteModel) {
        int count = countBlankNodes(deleteModel);
        log.print(LOGTAG.DEBUG, "" + count);

        // null pointer if we dont init this
        org.apache.jena.query.ARQ.init();

        // create a delete query
        UpdateBuilder builder = new UpdateBuilder();
        // builder.addDelete(deleteModel);
        String blank = addWhereClause2(builder, deleteModel);

        // create the outer sub query
        SelectBuilder outerSubBuilder = new SelectBuilder();
        addOuterSubQuery(outerSubBuilder, deleteModel, count, blank);

        // create the inner sub query
        SelectBuilder innerSubBuilder = new SelectBuilder();
        innerSubBuilder.addVar("?blank");
        addInnerSubQuery(innerSubBuilder, deleteModel, "?blank");

        // set sub queries
        outerSubBuilder.addSubQuery(innerSubBuilder);
        builder.addSubQuery(outerSubBuilder);

        log.print(logLevel, builder.buildRequest().toString());

    }

    // run blanknode update

    public void runBlankNodeUpdate(String pathToOldInstances, String pathToNewInstances, String pathToTemplates) {
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplates);
        log.print(logLevel, "----------------");
        msgs.printMessages();
        log.print(logLevel, "----------------");

        Diff d = new Diff(log);
        d.readDiff(pathToOldInstances, pathToNewInstances);
        log.print(logLevel, "Add linenumbers" + d.addLines.toString());
        log.print(logLevel, "delete linenumbers" + d.deleteLines.toString());

        String addInstancesString = null;
        String deleteInstancesString = null;
        try {
            addInstancesString = d.getAddInstancesString(pathToNewInstances);
            deleteInstancesString = d.getDeleteInstancesString(pathToOldInstances);
        } catch (FileNotFoundException error) {
            System.out.println("Could not old or new instance file");
            error.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.print(logLevel, "String containing instances to add\n'" + addInstancesString + "'");
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");

        OttrInterface jh = new OttrInterface(log);
        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        if (deleteModel != null) {
            log.print(logLevel, "delete model " + deleteModel.toString());
        }

        // createDelRequest(deleteModel);
        createDeleteRequest(deleteModel);
    }

}
