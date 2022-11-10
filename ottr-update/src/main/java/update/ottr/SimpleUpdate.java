package update.ottr;

import java.io.FileNotFoundException;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

import xyz.ottr.lutra.TemplateManager;

public class SimpleUpdate {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.DEFAULT;

    public SimpleUpdate(Logger log) {
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
        log.print(logLevel, "Update request:\n" + request.toString());
        return request;
    }

    public void runSimpleUpdate(TemplateManager tm, Logger log, String pathToNewInstances,
            String pathToOldInstances,
            String dbURL,
            Timer timer,
            int n) {
        
        timer.newSplit("start", "simple solution", n);
        Diff d = new Diff(log);
        d.readDiff( pathToOldInstances, pathToNewInstances);
        timer.newSplit("diff", "simple solution", n);
        log.print(logLevel, "Add linenumbers" + d.addLines.toString());
        log.print(logLevel, "delete linenumbers" + d.deleteLines.toString());

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

        log.print(logLevel, "String containing instances to add\n'" + addInstancesString + "'");
        log.print(logLevel, "String containing instances to delete\n'" + deleteInstancesString + "'");

        OttrInterface jh = new OttrInterface(log);
        Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
        Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

        timer.newSplit("model", "simple solution", n);

        log.print(logLevel, "delete model " + deleteModel.toString());

        try {
            FusekiInterface fi = new FusekiInterface(log);
            if (insertModel != null) {
                UpdateRequest insertRequest = createInsertRequest(insertModel);
                fi.updateLocalDB(insertRequest, dbURL);
            }
            if (deleteModel != null) {
                UpdateRequest deleteRequest = createDeleteRequest(deleteModel);
                fi.updateLocalDB(deleteRequest, dbURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        timer.newSplit("end", "simple solution", n);
    }
}