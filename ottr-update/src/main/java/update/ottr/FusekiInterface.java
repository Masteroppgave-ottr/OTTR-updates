package update.ottr;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jena.update.UpdateRequest;

public class FusekiInterface {

    public int updateLocalDB(UpdateRequest updateRequest, String dbURL) throws Exception {
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

        int res = con.getResponseCode();
        System.out.println("Response Code : " + res);
        return res;
    }
}
