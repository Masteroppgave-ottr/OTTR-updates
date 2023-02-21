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
            triples = resetDb(baseModel, newModel, dbURL);
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
        URL url = new URL(dbURL + "Rebuild");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "text/turtle");
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());

        log.print(logLevel, "Sending rebuild request to endpoint " + url);

        RDFDataMgr.write(out, rebuiltModel, RDFFormat.NTRIPLES);
        out.flush();
        out.close();

        int res = con.getResponseCode();
        log.print(logLevel, "Response Code from rebuild: " + res);
        if (res >= 400) {
            log.print(LOGTAG.WARNING, "Error, response from " + url.toString() + " resulted in a status code " + res
                    + " check whether the database is up and running.");
            return 1;
        }
        return res;

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
    public int resetDb(Model oldModel, Model newModel, String dbURL) throws IOException {
        long triples = oldModel.size();
        if (triples == 0) {
            log.print(LOGTAG.WARNING, "Error, the base model is empty.");
            return 1;
        }

        log.print(LOGTAG.DEBUG, "old model:");
        log.printModel(LOGTAG.DEBUG, oldModel);
        log.print(LOGTAG.DEBUG, "new model:");
        log.printModel(LOGTAG.DEBUG, newModel);

        URL url = new URL(dbURL + "Original");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "text/turtle");
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        RDFDataMgr.write(out, oldModel, RDFFormat.NTRIPLES);
        out.flush();
        out.close();
        log.print(logLevel, "Reset: " + url + " with " + triples + " triples");

        int res = con.getResponseCode();
        log.print(logLevel, "Response Code from reset Original: " + res);
        if (res >= 400) {
            log.print(LOGTAG.WARNING, "Error, response from " + url.toString() + " resulted in a status code " + res
                    + " check whether the database is up and running.");
            return 1;
        }

        url = new URL(dbURL + "Updated");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "text/turtle");
        con.setDoOutput(true);
        out = new DataOutputStream(con.getOutputStream());
        RDFDataMgr.write(out, oldModel, RDFFormat.NTRIPLES);
        out.flush();
        out.close();
        log.print(logLevel, "Reset: " + url + " with " + triples + " triples");

        res = con.getResponseCode();
        log.print(logLevel, "Response Code from reset Updated: " + res);

        url = new URL(dbURL + "Rebuild");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "text/turtle");
        con.setDoOutput(true);
        out = new DataOutputStream(con.getOutputStream());
        log.print(logLevel, "Reset: " + url);

        RDFDataMgr.write(out, newModel, RDFFormat.NTRIPLES);
        out.flush();
        out.close();

        res = con.getResponseCode();
        log.print(logLevel, "Response Code from reset Updated: " + res);

        return res;

    }
}
