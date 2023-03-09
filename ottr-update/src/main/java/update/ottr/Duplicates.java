package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
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
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;

import xyz.ottr.lutra.TemplateManager;

public class Duplicates {
  private Logger log;
  private String dbURL;
  private Timer timer;
  private TemplateManager tm;
  private LOGTAG logLevel = LOGTAG.DUPLICATE;
  private FusekiInterface fi;

  public Duplicates(Logger log, String dbURL, Timer timer, TemplateManager tm) {
    this.log = log;
    this.dbURL = dbURL;
    this.timer = timer;
    this.tm = tm;
    this.fi = new FusekiInterface(log);
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

  /**
   * Increments the counter of triples that already exist in the triple store.
   * If no counter exists, it is created and set to 2.
   * 
   * @param model the model containing the triples that already exist in the
   *              triple store
   */
  private void incrementCounterTriples(Model model) {
    UpdateBuilder updateBuilder = new UpdateBuilder();
    Node counterGraph = NodeFactory.createURI("localhost:3030/updated/count");
    WhereBuilder whereBuilder = new WhereBuilder();

    for (Statement statement : model.listStatements().toList()) {
      log.print(logLevel, "handling statement: " + statement);

      Resource innerTriple = model.createResource(statement);
      Property countPredicate = model.getProperty("http://example.org/count");
      updateBuilder.with(counterGraph);
      updateBuilder.addDelete(innerTriple, countPredicate, "?old_count");
      updateBuilder.addInsert(innerTriple, countPredicate,
          "?new_count");
      try {
        log.print(LOGTAG.DEBUG, statement.toString());

        Resource innerTripleString = createStringResourceFromStatement(statement, model);

        log.print(LOGTAG.DEBUG, "inner triple        " + innerTriple.toString());
        log.print(LOGTAG.DEBUG, "inner triple string " +
            innerTripleString.toString());
        whereBuilder.addOptional(innerTripleString, countPredicate, "?old_count");
        whereBuilder.addBind("IF (BOUND(?old_count), ?old_count + 1, 2)",
            "?new_count");
      } catch (ParseException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    updateBuilder.addWhere(whereBuilder);

    UpdateRequest request = updateBuilder.buildRequest();
    log.print(LOGTAG.DEBUG, "request:\n" + request.toString());

    try {
      fi.updateLocalDB(request, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
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

    log.print(LOGTAG.DEBUG, tripleString);
    try {
      constructBuilder.addFilter("?subject IN (" + tripleString + ")");
    } catch (ParseException e) {
      log.print(LOGTAG.ERROR, "error while creating the filter part of the query");
      e.printStackTrace();
    }

    Query query = constructBuilder.build();
    log.print(LOGTAG.DEBUG, "query:\n" + query.toString());

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

  private Model rdfRdfStarSetDifference(Model rdfModel, Model rdfStarModel) {

    // create a model from only the subject of all triples in rdfStarModel
    Model subjectModel = ModelFactory.createDefaultModel();
    StmtIterator rdfStarStmtIterator = rdfStarModel.listStatements();
    while (rdfStarStmtIterator.hasNext()) {
      Statement rdfStarStatement = rdfStarStmtIterator.next();

      Resource subjectResource = rdfStarStatement.getSubject();
      Statement subjectStatement = subjectResource.getStmtTerm();
      log.print(LOGTAG.DEBUG, subjectStatement.toString());
      subjectModel.add(subjectStatement);
    }

    log.print(LOGTAG.DEBUG, "subjectModel:");
    log.printModel(LOGTAG.DEBUG, subjectModel);
    log.print(LOGTAG.DEBUG, "rdfModel:");
    log.printModel(LOGTAG.DEBUG, rdfModel);

    Model differenceModel = ModelFactory.createDefaultModel();
    differenceModel.add(rdfModel);
    differenceModel.remove(subjectModel);
    log.print(LOGTAG.DEBUG, "differenceModel:");
    log.printModel(LOGTAG.DEBUG, differenceModel);

    return differenceModel;
  }

  /**
   * @param model the model to be deleted from the triple store
   */
  public void deleteModel(Model model) {
    // find counter triples
    Model counterModel = findCounterTriples(model);

    // decrement counter-triples

    // find non-counted triples
    Model nonDuplicatesModel = rdfRdfStarSetDifference(model, counterModel);

    // delete non-counted triples
    UpdateRequest request = new UpdateBuilder().addDelete(nonDuplicatesModel).buildRequest();
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
