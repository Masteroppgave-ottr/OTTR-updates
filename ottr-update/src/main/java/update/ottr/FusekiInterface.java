package update.ottr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
}
