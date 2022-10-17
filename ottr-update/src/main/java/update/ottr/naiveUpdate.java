package update.ottr;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;

public class naiveUpdate {

    public static UpdateRequest createDeleteRequest(Model oldModel)
    {
        UpdateBuilder builder = new UpdateBuilder()
            .addDelete(oldModel);
        return builder.buildRequest();
    }

    public static UpdateRequest createInsertRequest(Model newModel)
    {
        UpdateBuilder builder = new UpdateBuilder()
            .addInsert(newModel);
        return builder.buildRequest();
    }

    public static UpdateRequest createUpdateRequest(Model oldModel, Model newModel)
    {
        UpdateBuilder builder = new UpdateBuilder()
            .addDelete(oldModel)
            .addInsert(newModel)
            .addWhere("?x", "?y", "?z");
        return builder.buildRequest();
    }

}
