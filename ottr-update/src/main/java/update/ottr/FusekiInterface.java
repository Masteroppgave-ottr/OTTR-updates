package update.ottr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;

public class FusekiInterface {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.FUSEKI;

    public FusekiInterface(Logger log) {
        this.log = log;
    }

    /**
     * Populates the Original dataset on the Fuseki server with the expansion of
     * `instanceFileName`
     * Any existing data in the Original or Updated dataset is deleted.
     * 
     */
    public int initDB(String oldInstanceFileName, String newInstanceFileName, TemplateManager tm, String dbURL) {
        OttrInterface ottrInterface = new OttrInterface(log);

        Model baseModel = ottrInterface.expandAndGetModelFromFile(oldInstanceFileName, tm);
        Model newModel = ottrInterface.expandAndGetModelFromFile(newInstanceFileName, tm);

        int triples = 0;
        try {
            triples = fullResetDb(baseModel, newModel, dbURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return triples;
    }

    /**
     * Sends a SPARQL update request to the Fuseki server.
     * 
     * @param updateRequest
     *                      The SPARQL update request to send.
     * @param dbURL
     *                      The URL of the Fuseki server.
     */
    public int updateLocalDB(UpdateRequest updateRequest, String dbURL) throws MalformedURLException, IOException {
        // send post request to update local db
        URL url = new URL(dbURL + "Updated/update");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/sparql-update");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());

        log.print(logLevel, "Sending to endpoint " + url);
        log.print(logLevel, updateRequest.toString());

        out.writeBytes(updateRequest.toString());
        out.flush();
        out.close();

        int res = con.getResponseCode();
        log.print(logLevel, "Response Code : " + res);
        return res;
    }

    public Model getGraph(String dbURL, String graphName) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(dbURL + graphName, "TURTLE");
        return model;
    }

    private int putModel(Model model, String dbURL, String datasetName) throws MalformedURLException, IOException {
        URL url = new URL(dbURL + datasetName);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "text/turtle");
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        RDFDataMgr.write(out, model, RDFFormat.NTRIPLES);
        out.flush();
        out.close();

        int res = con.getResponseCode();
        log.print(logLevel, "Reset: " + url + datasetName + "| Response Code: " + res);

        if (res >= 400) {
            log.print(LOGTAG.WARNING, "Error, response from " + url.toString() + " resulted in a status code " + res
                    + " check whether the database is up and running.");
            return 1;
        }
        return res;
    }

    /**
     * Puts the new model into the Rebuild dataset on the Fuseki server
     * 
     * @param rebuiltModel
     *                     The model to put into the Rebuild dataset.
     * @param dbURL
     *                     The URL of the Fuseki server.
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public int rebuild(Model rebuiltModel, String dbURL) throws MalformedURLException, IOException {
        return putModel(rebuiltModel, dbURL, "Rebuild");
    }

    public int resetDb(Model oldModel, String dbURL) throws MalformedURLException, IOException {
        return putModel(oldModel, dbURL, "Updated");
    }

    /**
     * resets the database
     * 
     * @param oldModel
     *                 The model to put into Original and updated
     * @param dbURL
     *                 The URL of the Fuseki server.
     * @return
     * @throws IOException
     */
    public int fullResetDb(Model oldModel, Model newModel, String dbURL) throws IOException {
        long triples = oldModel.size();
        if (triples == 0) {
            log.print(LOGTAG.WARNING, "Error, the base model is empty.");
            return 1;
        }

        int res = 0;

        res = putModel(oldModel, dbURL, "Original");
        res = putModel(oldModel, dbURL, "Updated");
        res = putModel(newModel, dbURL, "Updated");

        return res;

    }
}
