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
        String tempDir = args[0];
        String instanceFileName = args[1];
        String templateFileName = args[2];
        String timerFileName = args[3];
        String dbURL = args[4];
        String[] N = args[5].split(", ");
        String[] total_changes = args[6].split(", ");

        LOGTAG[] logLevels = {
                // LOGTAG.DEFAULT,
                // LOGTAG.DEBUG,
                // LOGTAG.FUSEKI,
                // LOGTAG.OTTR,
                // LOGTAG.DIFF,
                // LOGTAG.WARNING
        };
        ArrayList<LOGTAG> loggerLevel = new ArrayList<LOGTAG>(List.of(logLevels));
        ArrayList<Solutions> solutions = new ArrayList<Solutions>(List.of(
                Solutions.REBUILD,
                Solutions.SIMPLE));

        Logger log = new Logger(loggerLevel);
        Timer timer = new Timer(tempDir + timerFileName);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), tempDir + templateFileName);
        msgs.printMessages();
        Controller controller = new Controller(solutions, log, timer, dbURL, tm);

        controller.nElements(N, tempDir + "generated/", instanceFileName);

        try {
            timer.writeSplitsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
