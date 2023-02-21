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

// This solution is about removing the local blank node assumption. 
public class BlankNode {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.BLANK;
    private String dbURL;
    private Timer timer;
    OttrInterface ottrInterface;

    public BlankNode(Logger log, String dbURL, Timer timer) {
        this.log = log;
        this.dbURL = dbURL;
        this.timer = timer;
        this.ottrInterface = new OttrInterface(log);
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
    private void addWhereClauseToSelect(SelectBuilder builder, Model model) {
        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            // if the statement contains a blank node
            Statement statement = statements.next();

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
    private String addWhereClause(UpdateBuilder builder, Model model) {
        StmtIterator statements = model.listStatements();
        // we store the name of the blank node so we can use it in the sub query
        String lastBlank = null;
        while (statements.hasNext()) {
            // if the statement contains a blank node
            Statement statement = statements.next();

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
                // TODO: handle multiple blank nodes later
                throw new RuntimeException("Multiple blank nodes not supported yet");
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
        addWhereClauseToSelect(builder, model);
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
            builder.addVar("count(" + blankName + ")", "count_" + blankName.substring(1));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        builder.addWhere(blankName, "?pred", "?obj");
        SelectBuilder newBuilder = new SelectBuilder().addWhere("?obj", "?pred", blankName);
        builder.addUnion(newBuilder);

        builder.addGroupBy(blankName);
        try {
            builder.addHaving("?count_" + blankName.substring(1) + "= " + count);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void findBlankInstances(String deleteInstancesString, TemplateManager tm) {
        OttrInterface jh = new OttrInterface(log);

        for (String line : deleteInstancesString.split("\n")) {
            Model m = jh.expandAndGetModelFromString(line, tm);
            int c = countBlankNodes(m);
            log.print(LOGTAG.DEBUG, "Count for " + line + " is " + c);
        }
    }

    public UpdateRequest createDeleteRequest(Model deleteModel) {
        int count = countBlankNodes(deleteModel);
        // null pointer if we dont init this
        org.apache.jena.query.ARQ.init();

        // create a delete query
        UpdateBuilder builder = new UpdateBuilder();
        // builder.addDelete(deleteModel);
        String blank = addWhereClause(builder, deleteModel);
        if (blank == null) {
            log.print(LOGTAG.BLANK, "No blank nodes found");
            return builder.buildRequest();
        }
        // create the outer sub query
        SelectBuilder outerSubBuilder = new SelectBuilder();
        addOuterSubQuery(outerSubBuilder, deleteModel, count, blank);

        // create the inner sub query
        SelectBuilder innerSubBuilder = new SelectBuilder();
        innerSubBuilder.addVar(blank);
        addInnerSubQuery(innerSubBuilder, deleteModel, blank);

        // set sub queries
        outerSubBuilder.addSubQuery(innerSubBuilder);
        outerSubBuilder.setLimit(1);
        builder.addSubQuery(outerSubBuilder);

        log.print(logLevel, builder.buildRequest().toString());
        return builder.buildRequest();
    }

    public UpdateRequest createInsertRequest(Model newModel) {
        UpdateBuilder builder = new UpdateBuilder()
                .addInsert(newModel);

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Insert request:\n" + request.toString());
        return request;
    }

    public void runBlankNodeUpdate(String pathToOldInstances, String pathToNewInstances, TemplateManager tm, int n,
            int changes) {
        timer.newSplit("start", "blank solution", n, changes);

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
        timer.newSplit("diff", "blank solution", n, changes);

        log.print(logLevel, "String containing instances to add\n'" + addInstancesString + "'");
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");

        findBlankInstances(deleteInstancesString, tm);

        OttrInterface jh = new OttrInterface(log);
        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        timer.newSplit("model", "blank solution", n, changes);

        if (deleteModel != null) {
            log.print(logLevel, "delete model " + deleteModel.toString());
        }
        if (insertModel != null) {
            log.print(logLevel, "insert model " + insertModel.toString());
        }

        try {
            FusekiInterface fi = new FusekiInterface(log);
            if (deleteModel != null) {
                UpdateRequest deleteRequest = createDeleteRequest(deleteModel);
                fi.updateLocalDB(deleteRequest, dbURL);
            }
            if (insertModel != null) {
                UpdateRequest insertRequest = createInsertRequest(insertModel);
                fi.updateLocalDB(insertRequest, dbURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        timer.newSplit("end", "blank solution", n, changes);

    }

    public UpdateRequest runShit(String deleteInstancesString, TemplateManager tm) {
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");
        UpdateBuilder builder = new UpdateBuilder();
        // null pointer if we dont init this
        org.apache.jena.query.ARQ.init();

        for (String line : deleteInstancesString.split("\n")) {
            Model m = ottrInterface.expandAndGetModelFromString(line, tm);

            int blankCount = countBlankNodes(m);
            log.print(LOGTAG.DEBUG, "Count for " + line + " is " + blankCount);

            String blank = addWhereClause(builder, m);
            // Create a sub query to match the blank node
            if (blankCount > 0) {
                // create the outer sub query
                SelectBuilder outerSubBuilder = new SelectBuilder();
                addOuterSubQuery(outerSubBuilder, m, blankCount, blank);

                // create the inner sub query
                SelectBuilder innerSubBuilder = new SelectBuilder();
                innerSubBuilder.addVar(blank);
                addInnerSubQuery(innerSubBuilder, m, blank);

                // set sub queries
                outerSubBuilder.addSubQuery(innerSubBuilder);
                outerSubBuilder.setLimit(1);
                builder.addSubQuery(outerSubBuilder);
            }
        }

        log.print(logLevel, builder.buildRequest().toString());
        return builder.buildRequest();
    }

    public void runBlankNodeUpdate2(String pathToOldInstances, String pathToNewInstances, TemplateManager tm, int n,
            int changes) {
        timer.newSplit("start", "blank solution", n, changes);

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
        timer.newSplit("diff", "blank solution", n, changes);

        log.print(logLevel, "String containing instances to add\n'" + addInstancesString + "'");
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");

        Model insertModel = ottrInterface.expandAndGetModelFromString(addInstancesString, tm);
        timer.newSplit("model", "blank solution", n, changes);

        if (insertModel != null) {
            log.print(logLevel, "insert model " + insertModel.toString());
        }

        try {
            FusekiInterface fi = new FusekiInterface(log);
            if (deleteInstancesString != "") {
                UpdateRequest deleteRequest = runShit(deleteInstancesString, tm);
                fi.updateLocalDB(deleteRequest, dbURL);
            }
            if (insertModel != null) {
                UpdateRequest insertRequest = createInsertRequest(insertModel);
                fi.updateLocalDB(insertRequest, dbURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        timer.newSplit("end", "blank solution", n, changes);

    }

}
