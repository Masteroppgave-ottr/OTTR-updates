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
import java.util.stream.Collectors;

import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.Result;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

public class App {

    private static Map<String, String> makePrefixes() {

        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("ex", "http://example.org/");
        return prefixes;
    }

    public static Model expandAndGetModel(String pathToInstances, TemplateManager tm) {
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

    private static Model expandAndGetModelFromString(String instanceString, TemplateManager tm) {
        SInstanceParser parser = new SInstanceParser(tm.getPrefixes().getNsPrefixMap(), new HashMap<>());
        ResultStream<Instance> instances = parser.parseString(instanceString);

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
        // String deleteQuery = naiveUpdate.createDeleteRequest(oldModel).toString();
        // writeToFile(deleteQuery, outputFileDelete);

        // String insertQuery = naiveUpdate.createInsertRequest(newModel).toString();
        // writeToFile(insertQuery, outputFileInsert);

        UpdateRequest deleteRequest = naiveUpdate.createDeleteRequest(oldModel);
        UpdateRequest insertRequest = naiveUpdate.createInsertRequest(newModel);

        // System.out.println(insertRequest.toString());

        try {
            updateLocalDB(deleteRequest, insertRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateLocalDB(UpdateRequest deleteRequest, UpdateRequest insertRequest, String dbURL)
            throws Exception {
        // send post request to update local db
        URL url = new URL(dbURL + "Updated/update");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/sparql-update");
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());

        System.out.println("sending request:\n" + deleteRequest.toString());
        out.writeBytes(deleteRequest.toString());

        out.flush();
        out.close();
        int status = con.getResponseCode();
        System.out.println(status);
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && java -jar target/update.jar
    {
        String pathToOldInstances = "../temp/old_instances.stottr";
        String pathToNewInstances = "../temp/new_instances.stottr";
        String pathToTemplate = "../temp/templates.stottr";
        String pathToDeleteQuery = "../temp/deleteQuery.rq";
        String pathToInsertQuery = "../temp/insertQuery.rq";
        String pathToUpdateQuery = "../temp/updateQuery.rq";
        String dbURL = "http://localhost:3030/";

        System.out.println("hello world!");

        // d.writeToFile("add.txt", "delete.txt");
        File outputFileDelete = new File(pathToDeleteQuery);
        File outputFileInsert = new File(pathToInsertQuery);
        File outputFileUpdate = new File(pathToUpdateQuery);

        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        msgs.printMessages();

        // Model oldModel = expandAndGetModel(pathToOldInstances, tm);
        // Model newModel = expandAndGetModel(pathToNewInstances, tm);

        // simpleUpdate(tm, oldModel, newModel, outputFileDelete, outputFileInsert);

        Diff d = new Diff();
        d.readDiffFromStdIn();

        System.out.println("lines to add from the 'new' file:\n" + d.addLines);
        System.out.println("lines to remove from the 'old' file:\n" + d.deleteLines);

        try {
            String add = d.getAddInstances(pathToNewInstances);
            System.out.println("add instances:\n" + add);
            Model m = expandAndGetModelFromString(add, tm);
            System.out.println("add model:\n" + m);
            String insertQuery = naiveUpdate.createInsertRequest(m).toString();
            System.out.println("insert query:\n" + insertQuery);
        } catch (FileNotFoundException e) {
            System.out.println("could not find file.\n" + e);
        }

        // try {
        // updateLocalDB(pathToDeleteQuery, pathToInsertQuery, dbURL);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        // String updateQuery = naiveUpdate.createUpdateQuery(oldModel/fil,
        // newModel/fil, updateDescrioption);
    }
}
