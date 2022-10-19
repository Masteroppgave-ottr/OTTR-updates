package update.ottr;

//lutra
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

//jena
import org.apache.jena.rdf.model.Model;

//java
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class JenaInterface {

    public Model expandAndGetModelFromFile(String pathToInstances, TemplateManager tm) {
        // read instances from file and expand them
        ResultStream<Instance> expanded = tm.readInstances(tm.getFormat("stOTTR"), pathToInstances)
                .innerFlatMap(tm.makeExpander());

        Set<Instance> instances = new HashSet<Instance>();
        expanded.innerForEach(instances::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instances);
        return writer.writeToModel();
    }

    public Model expandAndGetModelFromString(String instancesString, TemplateManager tm) {
        // read instances from string and expand them
        if (instancesString == null) {
            return null;
        }

        SInstanceParser parser = new SInstanceParser(tm.getPrefixes().getNsPrefixMap(), new HashMap<>());
        ResultStream<Instance> instances = parser.parseString(instancesString).innerFlatMap(tm.makeExpander());

        Set<Instance> instanceSet = new HashSet<Instance>();
        instances.innerForEach(instanceSet::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instanceSet);
        return writer.writeToModel();
    }

}
