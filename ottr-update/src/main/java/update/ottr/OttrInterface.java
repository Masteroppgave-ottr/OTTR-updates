package update.ottr;

//lutra
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.model.Instance;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.system.ResultStream;
import xyz.ottr.lutra.wottr.writer.WInstanceWriter;

//jena
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

//java
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
// import the file reader
import java.io.FileReader;

public class OttrInterface {
    private Logger log;
    private LOGTAG logLevel = LOGTAG.OTTR;

    public OttrInterface(Logger log) {
        this.log = log;
    }

    /**
     * Reads instances from a file and expands them to a Model
     * 
     * @param pathToInstances
     *                        The path to the file containing the instances. One
     *                        instance per
     *                        line. ended with a '.'
     * @param tm
     *                        The template manager. The template manager has read
     *                        the template file.
     * @return
     *         The model containing the expanded instances
     */
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

    /**
     * Reads instances from a string and expands them to a Model
     * 
     * @param instancesString
     *                        The string containing the instances. One instance per
     *                        line. ended with a .
     * @param tm
     *                        The template manager. The template manager has read
     *                        the template file
     * @return
     *         The model containing the expanded instances
     */
    public Model expandAndGetModelFromString(String instancesString, TemplateManager tm) {
        // read instances from string and expand them
        if (instancesString == null) {
            log.print(logLevel, "instancesString is null");
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

    public HashMap<Statement, Integer> expandAndGetCountedStatementsFromFile(String pathToInstances,
            TemplateManager tm) {

        HashMap<Statement, Integer> countedStatements = new HashMap<Statement, Integer>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pathToInstances));
            String line = reader.readLine();
            while (line != null) {
                log.print(LOGTAG.DEBUG, "line: " + line);
                if (line.length() > 0 && line.charAt(0) != '@') {
                    Model m = expandAndGetModelFromString(line, tm);
                    for (Statement s : m.listStatements().toList()) {
                        if (countedStatements.containsKey(s)) {
                            countedStatements.put(s, countedStatements.get(s) + 1);
                        } else {
                            countedStatements.put(s, 1);
                        }
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Object key : countedStatements.keySet().toArray()) {
            log.print(LOGTAG.DEBUG, key + " " + countedStatements.get(key));
        }

        return countedStatements;
    }
}
