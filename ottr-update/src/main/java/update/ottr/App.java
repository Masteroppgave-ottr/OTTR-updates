package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

//java
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void simpleUpdate(TemplateManager tm, Logger log, String pathToNewInstances,
            String pathToOldInstances,
            String dbURL) {

        naiveUpdate nu = new naiveUpdate(log);
        nu.simpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL);
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && diff <oldInstanceFile> <New instanceFile> | java -jar
    // target/update.jar
    //
    // Alternatively, you can run the following command in diff folder: make && make
    // diff
    {
        String pathToOldInstances = "../temp/old_instances.stottr";
        String pathToNewInstances = "../temp/new_instances.stottr";
        String pathToTemplate = "../temp/templates.stottr";
        String dbURL = "http://localhost:3030/";
        String[] logLevels = {
                "DEFAULT",
                // "FUSEKI",
                // "JENA",
                // "DIFF"
        };
        ArrayList<String> loggerLevel = new ArrayList<String>(List.of(logLevels));

        Logger log = new Logger(loggerLevel);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        msgs.printMessages();

        simpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL);

    }
}
