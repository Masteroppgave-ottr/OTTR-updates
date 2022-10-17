package update.ottr;

import xyz.ottr.lutra.api.StandardTemplateManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.net.*;
import java.io.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

public class App 
{

    public static Model expandAndGetModel(String pathToInstances, TemplateManager tm)
    {
        // read instances from file and expand them
        ResultStream<Instance> expanded = tm.readInstances(tm.getFormat("stOTTR"), pathToInstances).innerFlatMap(tm.makeExpander());
        Set<Instance> instances = new HashSet<Instance>();
        expanded.innerForEach(instances::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instances);
        return writer.writeToModel();
    }

    public static void writeToFile(String query, File file)
    {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(query);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void simpleUpdate(TemplateManager tm, Model oldModel, Model newModel, File outputFileDelete, File outputFileInsert, String dbURL)
    { 
        UpdateRequest updateRequest = naiveUpdate.createUpdateRequest(oldModel, newModel);

        try {
            updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateLocalDB(UpdateRequest updateRequest, String dbURL) throws Exception
    {
        // send post request to update local db
        URL url = new URL(dbURL+"Updated/update");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/sparql-update");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        
        out.writeBytes(updateRequest.toString());
        out.flush();
        out.close();
        int status = con.getResponseCode();
        System.out.println(status);
    }

    public static void main( String[] args )
    //to run: following command in ottr-update folder:
    //mvn package && java -jar target/update.jar
    {
        String pathToOldInstances ="../temp/old_instances.stottr";
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

        Model oldModel = expandAndGetModel(pathToOldInstances, tm);
        Model newModel = expandAndGetModel(pathToNewInstances, tm);

        simpleUpdate(tm, oldModel, newModel, outputFileDelete, outputFileInsert, dbURL);

        // String updateQuery = naiveUpdate.createUpdateQuery(oldModel/fil, newModel/fil, updateDescrioption);
    }
}
