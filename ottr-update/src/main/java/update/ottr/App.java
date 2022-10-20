package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;

import java.io.IOException;
//java
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void simpleUpdate(TemplateManager tm, Logger log, Timer timer, String pathToNewInstances,
            String pathToOldInstances,
            String dbURL) {

        naiveUpdate nu = new naiveUpdate(log);

        timer.newSplit("start", "naive solution", 5);
        nu.simpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL);
        timer.newSplit("end", "naive solution", 5);
        timer.writeSplitsStdout();
        log.print(LOGTAG.DEFAULT, timer.getDuration() + " ns");
        try {
            timer.writeSplitsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && diff <oldInstanceFilePath> <newInstanceFilePath> | java -jar
    // target/update.jar
    //
    // Alternatively, you can run the following command in diff folder: make && make
    // diff
    {
        String pathToOldInstances = "../temp/old_instances.stottr";
        String pathToNewInstances = "../temp/new_instances.stottr";
        String pathToTemplate = "../temp/templates.stottr";
        String dbURL = "http://localhost:3030/";
        String timerFile = "../temp/times.txt";
        LOGTAG[] logLevels = {
                LOGTAG.DEFAULT,
                // LOGTAG.DEBUG,
                // LOGTAG.FUSEKI,
                // LOGTAG.OTTR,
                // LOGTAG.DIFF
        };
        ArrayList<LOGTAG> loggerLevel = new ArrayList<LOGTAG>(List.of(logLevels));

        Logger log = new Logger(loggerLevel);
        Timer timer = new Timer(timerFile);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), pathToTemplate);
        msgs.printMessages();

        simpleUpdate(tm, log, timer, pathToNewInstances, pathToOldInstances, dbURL);
    }
}
