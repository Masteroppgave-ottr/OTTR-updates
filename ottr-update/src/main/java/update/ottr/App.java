package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

//java
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;

public class App {

    public static void buildRebuildSet(String pathToNewInstances, TemplateManager tm, Logger log,
            Timer timer, String dbURL) {
        OttrInterface ottrInterface = new OttrInterface(log);
        FusekiInterface fi = new FusekiInterface(log);

        timer.newSplit("start", "rebuild set", 5);

        Model model = ottrInterface.expandAndGetModelFromFile(pathToNewInstances, tm);
        try {
            fi.rebuild(model, dbURL);
            timer.newSplit("end", "rebuild set", 5);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && diff <oldInstanceFilePath> <newInstanceFilePath> | java -jar
    // target/update.jar
    //
    // Alternatively, you can run the following command in dev folder: make && make
    // diff
    {
        String source = args[0];
        String[] N = args[1].split(", ");
        String[] changes = args[2].split(", ");

        String pathToOldInstances = "../temp/old_instances.stottr";
        String pathToNewInstances = "../temp/new_instances.stottr";
        String pathToTemplate = "../temp/planet.stottr";
        String dbURL = "http://localhost:3030/";
        String timerFile = "../temp/times.txt";
        LOGTAG[] logLevels = {
                LOGTAG.DEFAULT,
                LOGTAG.DEBUG,
                // LOGTAG.FUSEKI,
                // LOGTAG.OTTR,
                // LOGTAG.DIFF,
                LOGTAG.WARNING
        };
        ArrayList<LOGTAG> loggerLevel = new ArrayList<LOGTAG>(List.of(logLevels));
        ArrayList<Solutions> solutions = new ArrayList<Solutions>(List.of(Solutions.REBUILD, Solutions.SIMPLE));

        Logger log = new Logger(loggerLevel);
        Timer timer = new Timer(timerFile);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        System.out.println("MANAGER" + tm.toString());
        msgs.printMessages();
        Controller controller = new Controller(solutions, log, timer, dbURL, tm);
        
        // controller.testSingleFile(pathToNewInstances, pathToOldInstances, 5);
        controller.nElements(N, changes, source);
        buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL);

        try {
            timer.writeSplitsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
