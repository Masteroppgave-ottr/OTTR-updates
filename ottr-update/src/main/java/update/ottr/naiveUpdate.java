package update.ottr;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

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
        log.print(logLevel, "Delete request: " + request.toString());
        return request;
    }

    public UpdateRequest createInsertRequest(Model newModel) {
        UpdateBuilder builder = new UpdateBuilder()
                .addInsert(newModel);

        UpdateRequest request = builder.buildRequest();
        log.print(logLevel, "Insert request: " + request.toString());
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
        log.print(logLevel, "Update request: " + request.toString());
        return request;
    }

}
