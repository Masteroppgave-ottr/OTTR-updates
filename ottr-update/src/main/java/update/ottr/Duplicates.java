package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
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

  public void insertModel(Model model) {

    for (Statement statement : model.listStatements().toList()) {
      log.print(logLevel, statement + "");
    }

    for (Statement statement : model.listStatements().toList()) {
      Resource subject = statement.getSubject();
      Property predicate = statement.getPredicate();
      RDFNode object = statement.getObject();
      // log.print(logLevel, statement + "");

      Resource r = new ResourceImpl("<" + subject + " " + predicate + " " + object + ">");
      model.add(r, predicate, object);

    }

    log.print(logLevel, "after:");

    for (Statement statement : model.listStatements().toList()) {
      log.print(logLevel, statement + "");
    }

    UpdateRequest request = new UpdateBuilder().addInsert(model).buildRequest();
    try {
      fi.updateLocalDB(request, dbURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
