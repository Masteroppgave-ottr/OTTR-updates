package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.update.UpdateRequest;
import org.h2.expression.Variable;

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

  private void findCounterTriples(Model model) {
    Model counterModel = ModelFactory.createDefaultModel();
    for (Statement statement : model.listStatements().toList()) {
      log.print(logLevel, "handling statement: " + statement);
      Property countPredicate = model.getProperty("http://example.org/count");
      Resource innerTriple = model.createResource(statement);
      RDFNode countVariable = model.createResource("?count");
      counterModel.add(innerTriple, countPredicate, countVariable);
    }

    UpdateBuilder updateBuilder = new UpdateBuilder();
    Node withGraph = NodeFactory.createURI("localhost:3030/updated/count");
    updateBuilder.addDelete(withGraph, counterModel);
    updateBuilder.addInsert(withGraph, counterModel);
    updateBuilder.addWhere("?s", "?p", "?o");
    UpdateRequest request = updateBuilder.buildRequest();

    log.print(LOGTAG.DEBUG, "request:\n" + request.toString());
  }

  private void findCounterTriples2(Model model) {
    UpdateBuilder updateBuilder = new UpdateBuilder();
    Node counterGraph = NodeFactory.createURI("localhost:3030/updated/count");
    WhereBuilder whereBuilder = new WhereBuilder();

    for (Statement statement : model.listStatements().toList()) {
      log.print(logLevel, "handling statement: " + statement);

      Resource innerTriple = model.createResource(statement);
      Property countPredicate = model.getProperty("http://example.org/count");

      updateBuilder.addDelete(counterGraph, "k:1", countPredicate, "?old_count");
      updateBuilder.addInsert(counterGraph, innerTriple, countPredicate, "?new_count");
      try {
        whereBuilder.addBind("IF (BOUND (?old_count), ?old_count + 1, 1)",
            "?new_count");
        // create a URI from innerTriple

        log.print(LOGTAG.DEBUG, statement.toString());

        Resource innerTripleString = model.createResource("< <" +
            statement.getSubject().toString() + "> <"
            + statement.getPredicate().toString() + "> \""
            + statement.getObject().toString() + "\" >");

        log.print(LOGTAG.DEBUG, "inner triple        " + innerTriple.toString());
        log.print(LOGTAG.DEBUG, "inner triple string " +
            innerTripleString.toString());

        whereBuilder.addOptional(innerTripleString, countPredicate, "?old_count");
      } catch (ParseException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    updateBuilder.addWhere(whereBuilder);
    UpdateRequest request = updateBuilder.buildRequest();
    log.print(LOGTAG.DEBUG, "request:\n" + request.toString());
  }

  public void insertModel(Model model) {
    Model duplicateModel = findDuplicates(model);
    if (duplicateModel != null) {
      log.print(logLevel, "duplicates found");
      findCounterTriples2(duplicateModel);
    }

    // for (Statement statement : model.listStatements().toList()) {
    // Property countPredicate = model.getProperty("http://example.org/count");
    // Resource innerTriple = model.createResource(statement);
    // model.add(innerTriple, countPredicate, "1^^xsd:integer");
    // }

    // UpdateBuilder updateBuilder = new UpdateBuilder();
    // Node withGraph = NodeFactory.createURI("localhost:3030/updated/count");
    // updateBuilder.addInsert(withGraph, model);
    // UpdateRequest request = updateBuilder.buildRequest();

    // try {
    // fi.updateLocalDB(request, dbURL);
    // } catch (MalformedURLException e) {
    // e.printStackTrace();
    // } catch (IOException e) {
    // e.printStackTrace();
    // }

  }
}
