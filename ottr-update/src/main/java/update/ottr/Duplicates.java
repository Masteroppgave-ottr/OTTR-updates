package update.ottr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;

public class Duplicates {
  private Logger log;
  private String dbURL;
  private Timer timer;
  private TemplateManager tm;
  private LOGTAG logLevel = LOGTAG.DUPLICATE;
  private FusekiInterface fi;
  private OttrInterface ottrInterface;

  public Duplicates(Logger log, String dbURL, Timer timer, TemplateManager tm) {
    this.log = log;
    this.dbURL = dbURL;
    this.timer = timer;
    this.tm = tm;
    this.fi = new FusekiInterface(log);
    this.ottrInterface = new OttrInterface(log);
  }

  /**
   * Queries the triple store for all triples in the model.
   * The triples that already exist in the triple store are returned in a model.
   */
  public Model findDuplicates(Model model) {
    ConstructBuilder constructBuilder = new ConstructBuilder()
        .addConstruct("?subject", "?predicate", "?object")
        .addValueVar("subject")
        .addValueVar("predicate")
        .addValueVar("object")
        .addWhere("?subject", "?predicate", "?object");

    constructBuilder.addValueVar("?subject");
    constructBuilder.addValueVar("?predicate");
    constructBuilder.addValueVar("?object");

    for (Statement statement : model.listStatements().toList()) {
      constructBuilder.addValueRow(statement.getSubject(), statement.getPredicate(), statement.getObject());
    }
    Query query = constructBuilder.build();

    Model duplicateModel = null;
    try {
      duplicateModel = fi.queryLocalDB(query, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    log.printModel(logLevel, duplicateModel);
    return duplicateModel;
  }

  /**
   * Turns a statement into a resource that with string manipulation
   * WARNING: This is a hack and does not take blank nodes into consideration.
   */
  private Resource createStringResourceFromStatement(Statement statement, Model model) {
    String object = statement.getObject().toString();
    if (statement.getObject().isLiteral()) {
      object = "'" + statement.getObject().asLiteral().getLexicalForm() + "'";
    } else {
      object = "<" + object + ">";
    }

    return model.createResource("< <" + statement.getSubject().toString() + "> <"
        + statement.getPredicate().toString() + "> " + object + " >");
  }

  private void incrementCounterTriples(Model model) {
    Node counterGraph = NodeFactory.createURI("localhost:3030/updated/count");
    UpdateBuilder updateBuilder = new UpdateBuilder()
        .addDelete("?subject", "<http://example.org/count>", "?old_count")
        .addInsert("?subject", "<http://example.org/count>", "?new_count")
        .with(counterGraph);

    WhereBuilder whereBuilder = new WhereBuilder();
    whereBuilder.addWhereValueVar("subject");
    for (Statement statement : model.listStatements().toList()) {
      Resource innerTriple = createStringResourceFromStatement(statement, model);
      whereBuilder.addWhereValueRow(innerTriple);
    }

    WhereBuilder optionalBindBuilder = new WhereBuilder();
    try {
      optionalBindBuilder
          .addOptional("?subject", "<http://example.org/count>",
              "?old_count")
          .addBind("IF (BOUND(?old_count), ?old_count + 1, 2)", "?new_count");
    } catch (ParseException e) {
      log.print(LOGTAG.ERROR, "could not create the BIND part of the query");
      e.printStackTrace();
    }

    updateBuilder.addWhere("?subject", "?predicate", "?object");
    String insertDeleteString = updateBuilder.buildRequest().toString();

    // remove the WHERE part
    insertDeleteString = insertDeleteString.substring(0,
        insertDeleteString.indexOf("WHERE"));
    // add the WHERE part from the whereBuilder
    insertDeleteString += whereBuilder.build().toString();
    // remove the last curly bracket
    insertDeleteString = insertDeleteString.substring(0,
        insertDeleteString.length() - 2);
    // remove the "WHERE {"" part of the optionalBindBuilder
    String optionalBindString = optionalBindBuilder.build().toString();
    optionalBindString = optionalBindString.substring(10);
    insertDeleteString += "\n" + optionalBindString;

    UpdateRequest request = UpdateFactory.create(insertDeleteString);
    log.print(logLevel, "incrementing/creating counter triples");
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Inserts a model into the triple store.
   * If the model contains triples that already exist in the triple store,
   * the counter of these triples is incremented.
   * 
   * @param model the model to be inserted into the triple store
   */
  public void insertModel(Model model) {
    // look for duplicates and increment their counter if necessary
    Model duplicateModel = findDuplicates(model);
    if (duplicateModel != null && duplicateModel.size() > 0) {
      log.print(logLevel, "duplicates found");
      incrementCounterTriples(duplicateModel);
    } else {
      log.print(logLevel, "no duplicates found");
    }

    // insert the model into the triple store
    log.print(logLevel, "inserting " + model.size() + " triples");
    UpdateRequest request = new UpdateBuilder().addInsert(model).buildRequest();
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Given a model of triples find the triples that already has a counter in the
   * triple store.
   */
  private Model findCounterTriples(Model model) {
    ConstructBuilder constructBuilder = new ConstructBuilder()
        .addConstruct("?subject", "?predicate", "?object")
        .addWhere("?subject", "?predicate", "?object")
        .from("localhost:3030/updated/count");

    String tripleString = "";
    for (Statement statement : model.listStatements().toList()) {
      Resource innerTriple = createStringResourceFromStatement(statement, model);
      tripleString += "<" + innerTriple.toString() + "> ,";
    }
    tripleString = tripleString.substring(0, tripleString.length() - 1);

    try {
      constructBuilder.addFilter("?subject IN (" + tripleString + ")");
    } catch (ParseException e) {
      log.print(LOGTAG.ERROR, "error while creating the filter part of the query");
      e.printStackTrace();
    }

    Query query = constructBuilder.build();

    Model duplicateModel = null;
    try {
      duplicateModel = fi.queryLocalDB(query, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (duplicateModel.size() > 0) {
      log.print(logLevel, "Found the following duplicates:");
      log.printModel(logLevel, duplicateModel);
    } else {
      log.print(logLevel, "No duplicates found");
    }
    return duplicateModel;
  }

  private Model rdfRdfStarSetDifference(Model rdfModel, Model rdfStarModel) {

    // create a model from only the subject of all triples in rdfStarModel
    Model subjectModel = ModelFactory.createDefaultModel();
    StmtIterator rdfStarStmtIterator = rdfStarModel.listStatements();
    while (rdfStarStmtIterator.hasNext()) {
      Statement rdfStarStatement = rdfStarStmtIterator.next();

      Resource subjectResource = rdfStarStatement.getSubject();
      Statement subjectStatement = subjectResource.getStmtTerm();
      subjectModel.add(subjectStatement);
    }

    Model differenceModel = ModelFactory.createDefaultModel();
    differenceModel.add(rdfModel);
    differenceModel.remove(subjectModel);

    return differenceModel;
  }

  private void deleteCountersLessThan(String n, Node graph) throws MalformedURLException, IOException {
    UpdateBuilder deleteBuilder = new UpdateBuilder();

    try {
      deleteBuilder.with(graph)
          .addDelete("?subject", "?predicate", "?duplicates")
          .addWhere("?subject", "?predicate", "?duplicates")
          .addFilter("?duplicates < " + n);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    UpdateRequest deleteRequest = deleteBuilder.buildRequest();

    fi.updateLocalDB(deleteRequest, dbURL);
  }

  /**
   * Decrements the counter of the triples in the model.
   * If the count is decremented to 1, the triple is removed from the triple
   * store.
   */
  private void decrementCounterTriples(Model model) {
    UpdateBuilder updateBuilder = new UpdateBuilder();
    WhereBuilder whereBuilder = new WhereBuilder();

    String tripleString = "";
    for (Statement statement : model.listStatements().toList()) {
      Resource innerTriple = createStringResourceFromStatement(statement.getSubject().getStmtTerm(), model);
      tripleString += "<" + innerTriple + "> ,";
    }
    tripleString = tripleString.substring(0, tripleString.length() - 1);

    try {
      whereBuilder.addWhere("?subject", "?predicate", "?old_count")
          .addFilter("?subject IN (" + tripleString + ")")
          .addBind("?old_count - 1", "?new_count");
    } catch (ParseException e) {
      log.print(LOGTAG.ERROR, "could not create the filter part of the query");
      e.printStackTrace();
    }

    Node counterGraph = NodeFactory.createURI("localhost:3030/updated/count");
    updateBuilder.with(
        counterGraph)
        .addDelete("?subject", "?predicate", "?old_count")
        .addInsert("?subject", "?predicate", "?new_count")
        .addWhere(whereBuilder);

    UpdateRequest request = updateBuilder.buildRequest();
    log.print(logLevel, "decrementing counter triples");
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // delete all triples from the counter graph that have a counter less than 2
    try {
      deleteCountersLessThan("2", counterGraph);
    } catch (IOException e) {
      log.print(LOGTAG.ERROR, "Error while deleting triples with count < 2 from the counter graph");
      e.printStackTrace();
    }
  }

  /**
   * @param model the model to be deleted from the triple store
   */
  public void deleteModel(Model model) {
    // find counter triples
    Model counterModel = findCounterTriples(model);

    // decrement counter-triples
    if (counterModel.size() > 0) {
      decrementCounterTriples(counterModel);
    }

    // find non-counted triples and delete them
    Model nonDuplicatesModel = rdfRdfStarSetDifference(model, counterModel);
    UpdateRequest request = new UpdateBuilder().addDelete(nonDuplicatesModel)
        .buildRequest();

    log.print(logLevel, "deleting " + nonDuplicatesModel.size() + " non-duplicates");
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runDuplicateUpdate(String pathToOldInstances, String pathToNewInstances, int n,
      int changes) {

    // Insert the old instances and insure that the count is correct before starting
    log.print(logLevel, "inserting instances from the old Instance file to prepare for the update");
    Model oldModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
    insertModel(oldModel);

    timer.newSplit("start", "duplicate solution", n, changes);

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
    timer.newSplit("diff", "duplicate solution", n, changes);

    log.print(logLevel, "String containing instances to add\n'" + addInstancesString + "'");
    log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");

    Model insertModel = ottrInterface.expandAndGetModelFromString(addInstancesString, tm);
    Model deletModel = ottrInterface.expandAndGetModelFromString(deleteInstancesString, tm);
    timer.newSplit("model", "duplicate solution", n, changes);

    try {
      if (deleteInstancesString != null) {
        deleteModel(deletModel);
      }
      if (insertModel != null) {
        insertModel(insertModel);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    timer.newSplit("end", "duplicate solution", n, changes);

  }
}
