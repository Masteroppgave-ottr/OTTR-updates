package update.ottr;

import java.io.FileNotFoundException;
import java.util.HashMap;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
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
    private HashMap<RDFNode, Integer> countBlankNodes(Model model) {

        // create a hashmap to store blank nodes and their count
        HashMap<RDFNode, Integer> blankNodes = new HashMap<RDFNode, Integer>();

        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();

            if (statement.getSubject().isAnon()) {
                if (blankNodes.containsKey(statement.getSubject())) {
                    blankNodes.put(statement.getSubject(), blankNodes.get(statement.getSubject()) + 1);
                } else {
                    blankNodes.put(statement.getSubject(), 1);
                }
            }
            if (statement.getObject().isAnon()) {
                if (blankNodes.containsKey(statement.getObject())) {
                    blankNodes.put(statement.getObject(), blankNodes.get(statement.getObject()) + 1);
                } else {
                    blankNodes.put(statement.getObject(), 1);
                }
            }
        }
        // return count;
        return blankNodes;
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
    private String addDeleteClause(UpdateBuilder builder, Model model) {
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
            // we give count a unique name in order to avoid name clashes with other
            // sub-queries
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

    public UpdateRequest createInsertRequest(Model newModel) {
        UpdateBuilder builder = new UpdateBuilder()
                .addInsert(newModel);

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Insert request:\n" + request.toString());
        return request;
    }

    public UpdateRequest createDeleteRequest(String deleteInstancesString, TemplateManager tm) {
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");
        UpdateBuilder builder = new UpdateBuilder();

        // we expand one instance at a time
        for (String line : deleteInstancesString.split("\n")) {
            Model m = ottrInterface.expandAndGetModelFromString(line, tm);

            HashMap<RDFNode, Integer> blankNodeCounts = countBlankNodes(m);

            log.print(LOGTAG.DEBUG, "Count for line " + line + " is :");
            for (RDFNode key : blankNodeCounts.keySet()) {
                log.print(LOGTAG.DEBUG, key + " : " + blankNodeCounts.get(key));
            }

            // TODO: må noe gjøres med denne funksjonen?
            addDeleteClause(builder, m);

            // create a sub query for each blank node
            for (RDFNode key : blankNodeCounts.keySet()) {
                String blankName = "?" + key.toString().replace("-", "_");
                log.print(LOGTAG.DEBUG, "key: " + key.toString() + " count: " + blankNodeCounts.get(key));
                // create the outer sub query
                SelectBuilder outerSubBuilder = new SelectBuilder();
                addOuterSubQuery(outerSubBuilder, m, blankNodeCounts.get(key), blankName);

                // create the inner sub query
                SelectBuilder innerSubBuilder = new SelectBuilder();
                innerSubBuilder.addVar(blankName);
                // TODO: don't add everything
                addInnerSubQuery(innerSubBuilder, m, blankName);

                // set sub queries
                outerSubBuilder.addSubQuery(innerSubBuilder);
                outerSubBuilder.setLimit(1);
                builder.addSubQuery(outerSubBuilder);
            }
        }

        log.print(logLevel, builder.buildRequest().toString());
        return builder.buildRequest();
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

        Model insertModel = ottrInterface.expandAndGetModelFromString(addInstancesString, tm);
        timer.newSplit("model", "blank solution", n, changes);

        if (insertModel != null) {
            log.print(logLevel, "insert model " + insertModel.toString());
        }

        try {
            FusekiInterface fi = new FusekiInterface(log);
            if (deleteInstancesString != null) {
                UpdateRequest deleteRequest = createDeleteRequest(deleteInstancesString, tm);
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
