package update.ottr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

public class Duplicates {
  private Logger log;
  private String dbURL;
  private Timer timer;
  private LOGTAG logLevel = LOGTAG.DUPLICATE;
  private FusekiInterface fi;
  private OttrInterface ottrInterface;

  public Duplicates(Logger log, String dbURL, Timer timer, OttrInterface ottrInterface) {
    this.log = log;
    this.dbURL = dbURL;
    this.timer = timer;
    this.fi = new FusekiInterface(log);
    this.ottrInterface = ottrInterface;
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

  public void incrementCounterTriples(HashMap<Statement, Integer> statementCountMap) {
    // find max value in the statementCountMap
    int max = 0;
    for (Statement statement : statementCountMap.keySet()) {
      if (statementCountMap.get(statement) > max) {
        max = statementCountMap.get(statement);
      }
    }

    for (int i = 1; i <= max; i++) {
      Node counterGraph = NodeFactory.createURI("localhost:3030/updated/count");
      UpdateBuilder updateBuilder = new UpdateBuilder()
          .addDelete("?subject", "<http://example.org/count>", "?old_count")
          .addInsert("?subject", "<http://example.org/count>", "?new_count")
          .with(counterGraph);

      Model emptyModel = ModelFactory.createDefaultModel();
      WhereBuilder whereBuilder = new WhereBuilder();
      whereBuilder.addWhereValueVar("subject");

      for (Statement statement : statementCountMap.keySet()) {
        Resource innerTriple = createStringResourceFromStatement(statement, emptyModel);

        if (statementCountMap.get(statement) > i) {
          whereBuilder.addWhereValueRow(innerTriple);
        }
      }

      WhereBuilder optionalBindBuilder = new WhereBuilder();
      try {
        optionalBindBuilder
            .addOptional("?subject", "<http://example.org/count>",
                "?old_count")
            .addBind("IF (BOUND(?old_count), ?old_count + 1, 2 )", "?new_count");
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

      if (statementCountMap.isEmpty()) {
        break;
      }
    }
  }

  public HashMap<Statement, Integer> combineAndGetDuplicates(Model model, HashMap<Statement, Integer> statementCount) {
    // add the model to the statementCount map
    for (Statement s : model.listStatements().toList()) {
      if (statementCount.containsKey(s)) {
        statementCount.put(s, statementCount.get(s) + 1);
      } else {
        statementCount.put(s, 2);
      }
    }

    // Only keep statements that have a count of 1
    HashMap<Statement, Integer> duplicateStatementMap = new HashMap<Statement, Integer>();
    for (Statement s : statementCount.keySet()) {
      if (statementCount.get(s) != 1) {
        duplicateStatementMap.put(s, statementCount.get(s));
      }
    }

    return duplicateStatementMap;
  }

  /**
   * Inserts a model into the triple store.
   * If the model contains triples that already exist in the triple store,
   * the counter of these triples is incremented.
   * 
   * @param model the model to be inserted into the triple store
   */
  public void insertFromString(String instancesString) {
    // look for duplicates between the instanceString and the triple store
    Model model = ottrInterface.expandAndGetModelFromString(instancesString);
    log.print(logLevel, "size of model: " + model.size());
    Model duplicateModel = findDuplicates(model);
    log.print(logLevel, "we have " + duplicateModel.size() + " duplicates between the model and the triple store");

    // look for duplicates inside the instanceString
    HashMap<Statement, Integer> countedStatements = ottrInterface.expandAndGetCountedStatementsFromString(
        instancesString);
    log.print(logLevel, "We have " + countedStatements.size() + " statements inside the instanceString");
    // print the contents of the countedStatements map
    for (Statement s : countedStatements.keySet()) {
      log.print(logLevel, s.toString() + " " + countedStatements.get(s));
    }

    HashMap<Statement, Integer> duplicateStatementMap = combineAndGetDuplicates(duplicateModel, countedStatements);
    log.print(logLevel, "We have " + duplicateStatementMap.size() + " duplicates");
    for (Statement s : duplicateStatementMap.keySet()) {
      log.print(logLevel, s.toString() + " " + duplicateStatementMap.get(s));
    }

    incrementCounterTriples(duplicateStatementMap);

    // insert the model into the triple store
    UpdateRequest request = new UpdateBuilder().addInsert(model).buildRequest();
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Inserts an instance file into the triple store.
   * Duplicates will be counted in the count graph.
   * 
   * @param instanceFileName the name of the instance file
   */
  public void insertFromFile(String instanceFileName) {

    // look for duplicates between the instanceString and the triple store
    Model model = ottrInterface.expandAndGetModelFromFile(instanceFileName);
    log.print(logLevel, "size of model: " + model.size());
    Model duplicateModel = findDuplicates(model);
    log.print(logLevel, "we have " + duplicateModel.size() + " duplicates between the model and the triple store");

    // look for duplicates inside the instanceString
    HashMap<Statement, Integer> countedStatements = ottrInterface.expandAndGetCountedStatementsFromFile(
        instanceFileName);
    log.print(logLevel, "We have " + countedStatements.size() + " statements inside the instanceString");
    // print the contents of the countedStatements map
    for (Statement s : countedStatements.keySet()) {
      log.print(logLevel, s.toString() + " " + countedStatements.get(s));
    }

    HashMap<Statement, Integer> duplicateStatementMap = combineAndGetDuplicates(duplicateModel, countedStatements);
    log.print(logLevel, "We have " + duplicateStatementMap.size() + " duplicates");
    for (Statement s : duplicateStatementMap.keySet()) {
      log.print(logLevel, s.toString() + " " + duplicateStatementMap.get(s));
    }

    incrementCounterTriples(duplicateStatementMap);

    // insert the model into the triple store
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
  public Model findCounterTriples(Model model) {
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

    // query the database
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
  private void decrementCounterTriples(HashMap<Statement, Integer> statementCountMap, Model counterModel) {
    // find max value of statementCountMap
    int maxCount = 0;
    for (Statement s : statementCountMap.keySet()) {
      int count = statementCountMap.get(s);
      if (count > maxCount) {
        maxCount = count;
      }
    }

    for (int i = 0; i < maxCount; i++) {
      UpdateBuilder updateBuilder = new UpdateBuilder();
      WhereBuilder whereBuilder = new WhereBuilder();

      // create a string of all triples that has a counter of i
      String tripleString = "";
      for (Statement s : counterModel.listStatements().toList()) {
        Statement triple = s.getSubject().getStmtTerm();
        Resource innerTriple = createStringResourceFromStatement(triple, counterModel);
        if (statementCountMap.getOrDefault(triple, -1) >= i) {
          tripleString += "<" + innerTriple + "> ,";
        }
      }

      // if the string is empty, we have no triples to decrement
      if (tripleString.length() == 0) {
        continue;
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
  }

  public void deleteFromModelAndCounter(HashMap<Statement, Integer> statementCountMap,
      Model counterModel, Model deleteModel) {
    log.print(logLevel, "The following triples will be deleted the following number of times:");
    for (Statement s : statementCountMap.keySet()) {
      log.print(logLevel, s.toString() + " : " + statementCountMap.get(s));
    }

    // decrement all counter triples
    log.print(logLevel, "decrementing counter triples if necessary");
    if (counterModel.size() > 0) {
      decrementCounterTriples(statementCountMap, counterModel);
    }

    // Remove the triples where we only need to decrement the counter
    for (Statement s : counterModel.listStatements().toList()) {
      Resource innerTriple = s.getSubject();
      int count = s.getObject().asLiteral().getInt();
      Statement innerStatement = innerTriple.getStmtTerm();
      if (statementCountMap.containsKey(innerStatement)) {
        if (statementCountMap.get(innerStatement) < count) {
          deleteModel.remove(innerStatement);
        }
      }
    }

    // execute the delete query
    UpdateRequest request = new UpdateBuilder().addDelete(deleteModel)
        .buildRequest();
    if (deleteModel.size() > 0) {
      log.print(logLevel, "deleting " + deleteModel.size() + " non-duplicates");
      try {
        fi.updateLocalDB(request, dbURL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Delete the result of the expansion of the given instance string from the
   * triple store.
   */
  public void deleteFromString(String instanceString) {
    // find all triples that have an existing counter-triple
    Model deleteModel = ottrInterface.expandAndGetModelFromString(instanceString);
    Model counterModel = findCounterTriples(deleteModel);

    // count the number of occurrences of each triple to be deleted
    HashMap<Statement, Integer> statementCountMap = ottrInterface
        .expandAndGetCountedStatementsFromString(instanceString);

    deleteFromModelAndCounter(statementCountMap, counterModel, deleteModel);
  }

  public void runDuplicateUpdate(String pathToOldInstances, String pathToNewInstances, int n,
      int changes) {
    if (n != -1) {
      timer.newSplit("start", "duplicate solution", n, changes);
    }

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
    if (n != -1) {
      timer.newSplit("diff", "duplicate solution", n, changes);
    }

    // TODO: do something about this timing
    if (n != -1) {
      timer.newSplit("model", "duplicate solution", n, changes);
    }

    try {
      if (deleteInstancesString != null) {
        deleteFromString(deleteInstancesString);
      }
      if (addInstancesString != null) {
        insertFromString(addInstancesString);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (n != -1) {
      timer.newSplit("end", "duplicate solution", n, changes);
    }
  }

}
