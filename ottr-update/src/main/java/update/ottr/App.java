package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

//jena
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

//java
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.net.*;
import java.io.*;

public class App {
    public static void simpleUpdate(TemplateManager tm, Model oldModel, Model newModel, File outputFileDelete,
            File outputFileInsert, String dbURL) {
        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(oldModel, newModel);

        try {
            updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateLocalDB(UpdateRequest updateRequest, String dbURL) throws Exception {
        // send post request to update local db
        URL url = new URL(dbURL + "Updated/update");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/sparql-update");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());

        System.out.println("To endpoint " + dbURL + "Updated/update");
        System.out.println("sending\n" + updateRequest.toString());

        out.writeBytes(updateRequest.toString());
        out.flush();
        out.close();
        int status = con.getResponseCode();
        System.out.println(status);
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

        JenaHelper jh = new JenaHelper();

        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        System.out.println("deleteModel: " + deleteModel);
        System.out.println("insertModel: " + insertModel);

        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(deleteModel, insertModel);

        try {
            updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
