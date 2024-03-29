package update.ottr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateFactory;
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
        OttrInterface ottrInterface = new OttrInterface(log, tm);

        Model baseModel = ottrInterface.expandAndGetModelFromFile(oldInstanceFileName);
        Model newModel = ottrInterface.expandAndGetModelFromFile(newInstanceFileName);

        int triples = 0;
        try {
            triples = fullResetDb(baseModel, newModel, dbURL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return triples;
    }

    /*
     * Read the response from a CONSTRUCT request to the Fuseki server.
     * The response is parsed into a Jena Model.
     */
    private Model parseConstructRequest(HttpURLConnection connection) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = connection.getInputStream();
        RDFDataMgr.read(model, is, Lang.TTL);
        return model;
    }

    /*
     * Send a SPARQL CONSTRUCT request to the Fuseki server.
     * The response is parsed into a Model and returned.
     */
    public Model queryLocalDB(Query query, String dbURL) throws IOException {
        URL url = new URL(dbURL + "Updated");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/sparql-query");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());

        log.print(logLevel, "Sending to endpoint " + url);
        log.print(logLevel, query.toString());

        // write query to output stream
        out.writeBytes(query.toString());

        out.flush();
        out.close();

        int res = con.getResponseCode();
        log.print(logLevel, "Response Code : " + res);

        return parseConstructRequest(con);
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

    /**
     * Get the default graph from the specified dataset
     */
    public Model getDataset(String dbURL, String datasetName) throws IOException {
        Model model = ModelFactory.createDefaultModel();

        URL url = new URL(dbURL + datasetName + "/data?graph=default");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/sparql-query");
        model.read(con.getInputStream(), null, "TTL");

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
        log.print(logLevel, "Reset: " + url + "| Response Code: " + res);

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

    /*
     * Resets the database by deleting all data from the Original, Rebuild and
     * Updated datasets.
     * Updated count Graph is added a triple
     */
    public void clearUpdated(String dbURL) throws MalformedURLException, IOException {
        putModel(ModelFactory.createDefaultModel(), dbURL, "Updated");

        // add a triple to the count graph in the Updated dataset. This creates the
        // graph and makes it possible to query it.
        UpdateRequest update = UpdateFactory.create(
                "INSERT DATA { GRAPH <localhost:3030/updated/count> { <http://example.com/ignoreMe> <http://example.com/ignoreMe> <http://example.com/ignoreMe> } }");

        updateLocalDB(update, dbURL);
    }

    public void resetUpdatedDataset(Model oldModel, String dbURL) throws MalformedURLException, IOException {
        putModel(oldModel, dbURL, "Updated");
        UpdateRequest update = UpdateFactory.create(
                "INSERT DATA { GRAPH <localhost:3030/updated/count> { <http://example.com/ignoreMe> <http://example.com/ignoreMe> <http://example.com/ignoreMe> } }");

        updateLocalDB(update, dbURL);
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

        // add a triple to the count graph in the Updated dataset. This creates the
        // graph and makes it possible to query it.
        UpdateRequest update = UpdateFactory.create(
                "INSERT DATA { GRAPH <localhost:3030/updated/count> { <http://example.com/ignoreMe> <http://example.com/ignoreMe> <http://example.com/ignoreMe> } }");

        updateLocalDB(update, dbURL);

        return res;

    }
}
