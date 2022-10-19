package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

//jena
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

//java
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class App {

    public static void simpleUpdate(TemplateManager tm, Logger log, String pathToNewInstances,
            String pathToOldInstances,
            String dbURL) {
        Diff d = new Diff(log);
        d.readDiffFromStdIn();

        log.print("DEFAULT", "Add linenumbers" + d.addLines.toString());
        log.print("DEFAULT", "delete linenumbers" + d.deleteLines.toString());

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

        log.print("DEFAULT", "String containing instances to add\n'" + addInstancesString + "'");
        log.print("DEFAULT", "String containing instances to delete\n'" + deleteInstancesString + "'");

        JenaInterface jh = new JenaInterface(log);

        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        naiveUpdate nu = new naiveUpdate(log);
        UpdateRequest updateRequest = nu.createUpdateRequest(deleteModel, insertModel);

        try {
            FusekiInterface fi = new FusekiInterface(log);
            fi.updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && diff <oldInstanceFile> <New instanceFile> | java -jar
    // target/update.jar
    //
    // Alternatively, you can run the following command in diff folder: make && make
    // diff
    {
        String pathToOldInstances = "../temp/old_instances.stottr";
        String pathToNewInstances = "../temp/new_instances.stottr";
        String pathToTemplate = "../temp/templates.stottr";
        String dbURL = "http://localhost:3030/";

        ArrayList<String> loggerLevel = new ArrayList<String>(List.of("FUSEKI", "JENA", "DIFF", "DEFAULT"));
        Logger log = new Logger(loggerLevel);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        msgs.printMessages();

        simpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL);

    }
}
