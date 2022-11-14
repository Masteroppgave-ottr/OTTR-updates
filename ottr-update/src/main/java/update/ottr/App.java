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
            Timer timer, String dbURL, String instances, String changes) {
        OttrInterface ottrInterface = new OttrInterface(log);
        FusekiInterface fi = new FusekiInterface(log);

        timer.newSplit("start", "rebuild set", Integer.parseInt(instances), Integer.parseInt(changes));

        Model model = ottrInterface.expandAndGetModelFromFile(pathToNewInstances, tm);
        try {
            fi.rebuild(model, dbURL);
            timer.newSplit("end", "rebuild set", Integer.parseInt(instances), Integer.parseInt(changes));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int[] combineStringNumberArrays(String[] arr1, String[] arr2, String[] arr3) {
        if (arr1.length != arr2.length || arr1.length != arr3.length) {
            throw new IllegalArgumentException("The arrays must have the same length");
        }

        int[] newArray = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            newArray[i] = Integer.parseInt(arr1[i]) + Integer.parseInt(arr2[i]) + Integer.parseInt(arr3[i]);
        }

        return newArray;
    }

    public static void main(String[] args)
    // to run: following command in ottr-update folder:
    // mvn package && diff <oldInstanceFilePath> <newInstanceFilePath> | java -jar
    // target/update.jar
    //
    // Alternatively, you can run the following command in dev folder: make && make
    // diff
    {
        String mode = args[0];
        String tempDir = args[1];
        String instanceFileName = args[2];
        String templateFileName = args[3];
        String timerFileName = args[4];
        String dbURL = args[5];
        String[] solutions = args[6].split(", ");
        System.out.println("mode: " + mode);

        LOGTAG[] logLevels = {
                // LOGTAG.DEFAULT,
                // LOGTAG.DEBUG,
                // LOGTAG.FUSEKI,
                // LOGTAG.OTTR,
                // LOGTAG.DIFF,
                // LOGTAG.WARNING
        };
        ArrayList<LOGTAG> loggerLevel = new ArrayList<LOGTAG>(List.of(logLevels));

        Logger log = new Logger(loggerLevel);
        Timer timer = new Timer(tempDir + timerFileName);
        TemplateManager tm = new StandardTemplateManager();
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), tempDir + templateFileName);
        msgs.printMessages();
        Controller controller = new Controller(solutions, log, timer, dbURL, tm);

        if (mode.equals("n=instances")) {
            String[] instances = args[7].split(", ");
            String changeNr = args[8];
            controller.nInstances(instances, tempDir + "generated/", instanceFileName, changeNr);
        }
        if (mode.equals("n=changes")) {
            String instances = args[7];
            String[] deletions = args[8].split(", ");
            String[] changes = args[9].split(", ");
            String[] insertions = args[10].split(", ");
            int[] changeList = combineStringNumberArrays(deletions, changes, insertions);

            controller.nChanges(changeList, tempDir + "generated/", instanceFileName, instances);
        }
        try {
            timer.writeSplitsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
