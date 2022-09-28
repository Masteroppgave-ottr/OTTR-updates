package update.ottr;

import xyz.ottr.lutra.api.StandardTemplateManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;

import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

public class App 
{

    public static Model expandAndGetModel(String pathToInstances, TemplateManager tm)
    {
        // read instances from file and expand them
        ResultStream<Instance> expanded = tm.readInstances(tm.getFormat("stOTTR"), pathToInstances).innerFlatMap(tm.makeExpander());
        Set<Instance> instances = new HashSet<Instance>();
        expanded.innerForEach(instances::add);

        // write expanded instances to model
        WInstanceWriter writer = new WInstanceWriter();
        writer.addInstances(instances);
        return writer.writeToModel();
    }

    public static void main( String[] args )
    //to run: following command in ottr-update folder:
    //mvn package && java -jar target/update.jar
    {
        String pathToOldInstances = "/home/prebz/ottr/dev/temp/old_instances.stottr";
        String pathToNewInstances = "/home/prebz/ottr/dev/temp/new_instances.stottr";
        String pathToTemplate = "/home/prebz/ottr/dev/temp/templates.stottr";

        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate); 
        msgs.printMessages();

        Model oldInstances = expandAndGetModel(pathToOldInstances, tm);
        Model newInstances = expandAndGetModel(pathToNewInstances, tm);
        
        // StmtIterator iterator = oldInstances.listStatements();
        // for ( ; iterator.hasNext() ; ) {
        //     System.out.println(iterator.nextStatement());
        // }

        // System.out.println(naiveUpdate.createQuery(oldExpanded, newExpanded));
    }
}
