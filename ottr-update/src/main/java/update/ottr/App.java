package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

//jena
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

//java
import java.io.File;
import java.io.*;

public class App {
    public static void simpleUpdate(TemplateManager tm, Model oldModel, Model newModel, File outputFileDelete,
            File outputFileInsert, String dbURL) {
        FusekiInterface fi = new FusekiInterface();
        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(oldModel, newModel);

        try {
            fi.updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void simpleUpdate(TemplateManager tm, String pathToNewInstances, String pathToOldInstances,
            String dbURL) {
        Diff d = new Diff();
        d.readDiffFromStdIn();

        System.out.println(d.addLines);
        System.out.println(d.deleteLines);

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

        System.out.println("addInstancesString: " + addInstancesString);
        System.out.println("deleteInstancesString: " + deleteInstancesString);

        JenaInterface jh = new JenaInterface();

        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        System.out.println("deleteModel: " + deleteModel);
        System.out.println("insertModel: " + insertModel);

        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(deleteModel, insertModel);

        try {
            FusekiInterface fi = new FusekiInterface();
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

        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        msgs.printMessages();

        simpleUpdate(tm, pathToNewInstances, pathToOldInstances, dbURL);

    }
}
