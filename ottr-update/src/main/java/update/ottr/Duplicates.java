package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.handlers.ValuesHandler;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.update.UpdateRequest;

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

  public void findDuplicates(Model model) {
    SelectBuilder selectBuilder = new SelectBuilder()
        .addVar("subject")
        .addVar("predicate")
        .addVar("object")
        .addWhere("?s", "?p", "?o");

    // selectBuilder.addWhereValueRow("?s", "?p", "?o", "?count");
    // create a var
    Var subject = Var.alloc("geir");
    Var predicate = Var.alloc("predicate");
    Var object = Var.alloc("object");
    selectBuilder.addValueVar(subject);
    selectBuilder.addValueVar(predicate);
    selectBuilder.addValueVar(object);
    selectBuilder.addValueRow(subject, predicate, object);
    Query query = selectBuilder.build();

    // create

    try {
      fi.queryLocalDB(query, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void insertModel(Model model) {

    findDuplicates(model);

    for (Statement statement : model.listStatements().toList()) {
      Property countPredicate = model.getProperty("http://example.org/count");
      Resource innerTriple = model.createResource(statement);
      model.add(innerTriple, countPredicate, "1^^xsd:integer");
    }

    UpdateBuilder updateBuilder = new UpdateBuilder();
    Node withGraph = NodeFactory.createURI("localhost:3030/updated/count");
    updateBuilder.addInsert(withGraph, model);
    UpdateRequest request = updateBuilder.buildRequest();

    try {
      fi.updateLocalDB(request, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
