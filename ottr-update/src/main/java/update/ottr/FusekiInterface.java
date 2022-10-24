package update.ottr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateRequest;

public class FusekiInterface {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.FUSEKI;

    public FusekiInterface(Logger log) {
        this.log = log;
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

        RDFDataMgr.write(out, rebuiltModel, RDFFormat.TURTLE_BLOCKS);
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
}
