package update.ottr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateRequest;

public class Combined {
  private Logger log;
  private LOGTAG logLevel = LOGTAG.COMBINED;
  private String dbURL;
  private Timer timer;
  OttrInterface ottrInterface;
  Duplicates duplicates;
  BlankNode blankNodes;
  FusekiInterface fi;
  Scanner scanner;

  public Combined(Logger log, String dbURL, Timer timer, OttrInterface ottrInterface) {
    this.log = log;
    this.dbURL = dbURL;
    this.timer = timer;
    this.ottrInterface = ottrInterface;
    this.duplicates = new Duplicates(log, dbURL, timer, ottrInterface);
    this.blankNodes = new BlankNode(log, dbURL, timer, ottrInterface);
    this.fi = new FusekiInterface(log);
    this.scanner = new Scanner(System.in);
  }

  @SuppressWarnings("unused")
  private void userBreakpoint() {
    System.out.println("Press Enter to continue...");
    scanner.nextLine();
  }

  public void insertFromFile(String file) {
    String fileContent = ottrInterface.readFromFileToString(file, 6);
    insertFromString(fileContent);
  }

  public void insertFromString(String instanceString) {
    Model allTriples = ottrInterface.expandAndGetModelFromString(instanceString);
    log.print(LOGTAG.COMBINED, "all triples are");
    log.printModel(LOGTAG.COMBINED, allTriples);
    Model nonBlankTriples = ModelFactory.createDefaultModel();
    Model blankTriples = ModelFactory.createDefaultModel();
    for (StmtIterator i = allTriples.listStatements(); i.hasNext();) {
      Statement s = i.nextStatement();
      if (s.asTriple().getSubject().isBlank() || s.asTriple().getObject().isBlank()) {
        blankTriples.add(s);
      } else {
        nonBlankTriples.add(s);
      }
    }

    log.print(LOGTAG.DEBUG, "nonBlankTriples: " + nonBlankTriples.size());
    log.printModel(LOGTAG.DEBUG, nonBlankTriples);
    log.print(LOGTAG.DEBUG, "blankTriples: " + blankTriples.size());
    log.printModel(LOGTAG.DEBUG, blankTriples);

    // ~~ Handle blank triples ~~
    if (blankTriples.size() > 0) {
      log.print(LOGTAG.COMBINED, "INSERTING " + blankTriples.size() + " BLANK TRIPLES");
      UpdateRequest blankInsertRequest = blankNodes.createInsertRequest(blankTriples);
      try {
        fi.updateLocalDB(blankInsertRequest, dbURL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // ~~ Handle non blank triples ~~
    if (nonBlankTriples.size() > 0) {
      log.print(LOGTAG.COMBINED, "INSERTING " + nonBlankTriples.size() + " NON BLANK TRIPLES");
      Model duplicateModel = duplicates.findDuplicates(nonBlankTriples);
      log.print(logLevel, "we have " + duplicateModel.size() + " duplicates between the model and the triple store");

      HashMap<Statement, Integer> countedStatements = ottrInterface.expandAndGetCountedStatementsFromString(
          instanceString);

      // remove all key value pairs where the key is a blank node
      HashMap<Statement, Integer> nonBlankCountedStatements = new HashMap<Statement, Integer>();
      for (Statement key : countedStatements.keySet()) {
        if (!key.asTriple().getSubject().isBlank() && !key.asTriple().getObject().isBlank()) {
          nonBlankCountedStatements.put(key, countedStatements.get(key));
        }
      }
      log.print(logLevel, "We have " + nonBlankCountedStatements.size() + " statements inside the instanceString");

      HashMap<Statement, Integer> duplicateStatementMap = duplicates.combineAndGetDuplicates(duplicateModel,
          nonBlankCountedStatements);
      log.print(logLevel, "We have " + duplicateStatementMap.size() + " duplicates");
      for (Statement s : duplicateStatementMap.keySet()) {
        log.print(logLevel, s.toString() + " " + duplicateStatementMap.get(s));
      }

      // increment counters if there is any
      duplicates.incrementCounterTriples(duplicateStatementMap);

      // insert the non blank triples
      UpdateRequest request = new UpdateBuilder().addInsert(nonBlankTriples).buildRequest();
      try {
        fi.updateLocalDB(request, dbURL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void deleteFromString(String instanceString) {
    // create delete the blank triples
    UpdateRequest blankDeleteRequest = createDeleteRequestBlankTriples(instanceString);
    if (blankDeleteRequest != null) {
      log.print(LOGTAG.COMBINED, "DELETING BLANK TRIPLES");
      try {
        fi.updateLocalDB(blankDeleteRequest, dbURL);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // count the number of occurrences of each triple to be deleted
    HashMap<Statement, Integer> statementCountMap = ottrInterface
        .expandAndGetCountedStatementsFromString(instanceString);

    Model nonBlankModel = ModelFactory.createDefaultModel();
    // only keep non blank triples
    for (Statement key : statementCountMap.keySet()) {
      if (!key.asTriple().getSubject().isBlank() && !key.asTriple().getObject().isBlank()) {
        nonBlankModel.add(key);
      }
    }

    log.print(LOGTAG.DEBUG, "nonBlankModel: " + nonBlankModel.size());
    log.printModel(LOGTAG.DEBUG, nonBlankModel);
    if (nonBlankModel.size() > 0) {
      log.print(LOGTAG.COMBINED, "DELETING NON BLANK TRIPLES");
      Model counterModel = duplicates.findCounterTriples(nonBlankModel);
      log.print(LOGTAG.DEBUG, "counterModel: " + counterModel.size());
      log.printModel(LOGTAG.DEBUG, counterModel);

      duplicates.deleteFromModelAndCounter(statementCountMap, counterModel, nonBlankModel);
    }
  }

  public UpdateRequest createDeleteRequestBlankTriples(String instancesString) {
    UpdateBuilder builder = new UpdateBuilder();
    boolean hasBlanks = false;
    log.print(LOGTAG.COMBINED, "Creating the delete request for the blank triples in\n" + instancesString);
    for (String line : instancesString.split("\n")) {
      Model m = ottrInterface.expandAndGetModelFromString(line);
      log.print(LOGTAG.COMBINED, "!!! THE MODEL IS NOW !!!");
      log.printModel(LOGTAG.COMBINED, m);
      log.print(LOGTAG.COMBINED, "");
      HashMap<RDFNode, Integer> blankNodeCounts = blankNodes.countBlankNodes(m);
      boolean thisHasBlanks = blankNodes.addDeleteClauseOnlyBlanks(builder, m);
      hasBlanks = hasBlanks || thisHasBlanks;

      // create a sub query for each blank node
      for (RDFNode key : blankNodeCounts.keySet()) {
        String blankName = "?" + key.toString().replace("-", "_");

        // create the outer sub query
        SelectBuilder outerSubBuilder = new SelectBuilder();
        blankNodes.addOuterSubQuery(outerSubBuilder, m, blankNodeCounts.get(key), blankName);

        // create the inner sub query
        SelectBuilder innerSubBuilder = new SelectBuilder();
        blankNodes.addInnerSubQuery(innerSubBuilder, m, blankName);

        // set sub queries
        outerSubBuilder.addSubQuery(innerSubBuilder);
        builder.addSubQuery(outerSubBuilder);
      }
    }

    if (!hasBlanks) {
      log.print(LOGTAG.COMBINED, "No blank nodes found in the instances string");
      return null;
    }
    log.print(LOGTAG.DEBUG, builder.buildRequest().toString());
    return builder.buildRequest();

  }

  public void runCombinedUpdate(String pathToOldInstances, String pathToNewInstances, int n, int changes) {
    if (n != -1) {
      timer.newSplit("start", "combined solution", n, changes);
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
      timer.newSplit("diff", "combined solution", n, changes);
    }

    // TODO: do something about this timing
    if (n != -1) {
      timer.newSplit("model", "combined solution", n, changes);
    }

    log.print(logLevel, "Add instances string: " + addInstancesString);
    log.print(logLevel, "Delete instances string: " + deleteInstancesString);

    if (deleteInstancesString != null) {
      deleteFromString(deleteInstancesString);
    }

    if (addInstancesString != null) {
      insertFromString(addInstancesString);
    }

    if (n != -1) {
      timer.newSplit("end", "combined solution", n, changes);
    }

  }
}