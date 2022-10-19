package update.ottr;

import java.io.FileNotFoundException;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;

public class naiveUpdate {
    private Logger log;
    private String logLevel = "DEFAULT";

    public naiveUpdate(Logger log) {
        this.log = log;
    }

    public UpdateRequest createDeleteRequest(Model oldModel) {
        UpdateBuilder builder = new UpdateBuilder()
                .addDelete(oldModel);

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Delete request:\n" + request.toString());
        return request;
    }

    public UpdateRequest createInsertRequest(Model newModel) {
        UpdateBuilder builder = new UpdateBuilder()
                .addInsert(newModel);

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Insert request:\n" + request.toString());
        return request;
    }

    public UpdateRequest createUpdateRequest(Model oldModel, Model newModel) {
        UpdateBuilder builder = new UpdateBuilder().addWhere("?x", "?y", "?z");
        ;

        if (oldModel != null) {
            builder.addDelete(oldModel);
        }
        if (newModel != null) {
            builder.addInsert(newModel);
        }

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Update request:+\n" + request.toString());
        return request;
    }

    public void simpleUpdate(TemplateManager tm, Logger log, String pathToNewInstances,
            String pathToOldInstances,
            String dbURL) {
        Diff d = new Diff(log);
        d.readDiffFromStdIn();

        log.print("DEFAULT", "Add linenumbers" + d.addLines.toString());
        log.print("DEFAULT", "delete linenumbers" + d.deleteLines.toString());

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

        log.print("DEFAULT", "String containing instances to add\n'" + addInstancesString + "'");
        log.print("DEFAULT", "String containing instances to delete\n'" + deleteInstancesString + "'");

        JenaInterface jh = new JenaInterface(log);
        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        naiveUpdate nu = new naiveUpdate(log);
        UpdateRequest updateRequest = nu.createUpdateRequest(deleteModel, insertModel);

        try {
            FusekiInterface fi = new FusekiInterface(log);
            fi.updateLocalDB(updateRequest, dbURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
