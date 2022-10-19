package update.ottr;

import xyz.ottr.lutra.api.StandardTemplateManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.net.*;
import java.io.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

import java.util.stream.Collectors;

import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.Result;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

public class App {
    public static Model expandAndGetModelFromFile(String pathToInstances, TemplateManager tm) {
        // read instances from file and expand them
        ResultStream<Instance> expanded = tm.readInstances(tm.getFormat("stOTTR"), pathToInstances)
                .innerFlatMap(tm.makeExpander());

        Set<Instance> instances = new HashSet<Instance>();
        expanded.innerForEach(instances::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instances);
        return writer.writeToModel();
    }

    private static Model expandAndGetModelFromString(String instancesString, TemplateManager tm) {
        // read instances from string and expand them
        if (instancesString == null) {
            return null;
        }

        SInstanceParser parser = new SInstanceParser(tm.getPrefixes().getNsPrefixMap(), new HashMap<>());
        ResultStream<Instance> instances = parser.parseString(instancesString).innerFlatMap(tm.makeExpander());

        Set<Instance> instanceSet = new HashSet<Instance>();
        instances.innerForEach(instanceSet::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instanceSet);
        return writer.writeToModel();
    }

    public static void writeToFile(String query, File file) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(query);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        String pathToDeleteQuery = "../temp/deleteQuery.rq";
        String pathToInsertQuery = "../temp/insertQuery.rq";
        String pathToUpdateQuery = "../temp/updateQuery.rq";
        String dbURL = "http://localhost:3030/";

        File outputFileDelete = new File(pathToDeleteQuery);
        File outputFileInsert = new File(pathToInsertQuery);
        File outputFileUpdate = new File(pathToUpdateQuery);

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

        Model insertModel = expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = expandAndGetModelFromString(deleteInstancesString, tm);

        System.out.println("deleteModel: " + deleteModel);
        System.out.println("insertModel: " + insertModel);

        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(deleteModel, insertModel);

        try {
            System.out.println("updateRequest: " + updateRequest);
            updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
