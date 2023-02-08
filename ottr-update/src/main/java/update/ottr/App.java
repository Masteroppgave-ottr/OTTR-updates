package update.ottr;

//lutra
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.TemplateManager;
import xyz.ottr.lutra.system.MessageHandler;
import xyz.ottr.lutra.system.Message.Severity;

import org.apache.jena.rdf.model.Model;

import java.io.FileNotFoundException;
//java
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

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

    private static int populateDB(Logger log, FusekiInterface fi, String pathToNewInstances, TemplateManager tm,
            String dbURL) {
        int inserted_triples = fi.initDB(pathToNewInstances, tm, dbURL);
        log.print(LOGTAG.FUSEKI, "Inserted " + inserted_triples + " triples into the triple store.");
        if (inserted_triples == 1) {
            log.print(LOGTAG.ERROR, "Error while inserting the initial data into the triple store.");
            log.print(LOGTAG.ERROR,
                    "Make sure the INSTANCE_FILE variable is set correctly in the Makefile, and that it is located in the TEMP_DIR");
            System.exit(1);
        }
        if (inserted_triples == 0) {
            log.print(LOGTAG.ERROR, "Error while connecting to triple store");
            log.print(LOGTAG.ERROR,
                    "Make sure you have started the server correctly with the 'make init_db' command");
            System.exit(1);
        }
        return inserted_triples;
    }

    public static void main(String[] args) {
        String mode = args[0];
        String tempDir = args[1];
        String instanceFileName = args[2];
        String templateFileName = args[3];
        String timerFileName = args[4];
        String dbURL = args[5];
        String[] solutions = args[6].split(", ");

        LOGTAG[] logLevels = {
                LOGTAG.DEFAULT,
                LOGTAG.DEBUG,
                LOGTAG.FUSEKI,
                LOGTAG.OTTR,
                LOGTAG.DIFF,
                LOGTAG.WARNING,
                LOGTAG.ERROR,
                LOGTAG.BLANK
        };
        ArrayList<LOGTAG> loggerLevel = new ArrayList<LOGTAG>(List.of(logLevels));

        // init objects
        Logger log = new Logger(loggerLevel);
        Timer timer = new Timer(tempDir + timerFileName);
        TemplateManager tm = new StandardTemplateManager();
        FusekiInterface fi = new FusekiInterface(log);

        // read the template file
        MessageHandler msgs = tm.readLibrary(tm.getFormat("stOTTR"), tempDir +
                templateFileName);
        Severity severity = msgs.printMessages();
        if (severity == Severity.ERROR) {
            log.print(LOGTAG.ERROR, "Error while reading the template file.");
            log.print(LOGTAG.ERROR,
                    "Make sure the TEMPLATE_FILE variable is set correctly in the Makefile, and that it is located in the TEMP_DIR");
            System.exit(1);
        }

        Controller controller = new Controller(solutions, log, timer, dbURL, tm);
        if (mode.equals("default")) {
            String old_instance_fileName = tempDir + "old_" + instanceFileName;
            String new_instance_fileName = tempDir + "new_" + instanceFileName;
            populateDB(log, fi, new_instance_fileName, tm, dbURL);

            Diff d = new Diff(log);
            d.readDiff(old_instance_fileName, new_instance_fileName);

            String addInstancesString = null;
            String deleteInstancesString = null;
            try {
                addInstancesString = d.getAddInstancesString(new_instance_fileName);
                deleteInstancesString = d.getDeleteInstancesString(old_instance_fileName);
            } catch (FileNotFoundException error) {
                System.out.println("Could not old or new instance file");
                error.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            OttrInterface jh = new OttrInterface(log);
            // models for the old and new instance files
            Model oldModel = jh.expandAndGetModelFromFile(old_instance_fileName, tm);
            Model newModel = jh.expandAndGetModelFromFile(new_instance_fileName, tm);

            // models for the changes
            Model insertModel = jh.expandAndGetModelFromString(addInstancesString, tm);
            Model deleteModel = jh.expandAndGetModelFromString(deleteInstancesString, tm);

            // INSERT YOUR CODE HERE
        }

        if (mode.equals("n=instances")) {
            // parse extra arguments
            String[] instances = args[7].split(", ");
            String changeNr = args[8];
            controller.nInstances(instances, tempDir + "generated/", instanceFileName,
                    changeNr);
        }
        if (mode.equals("n=changes")) {
            // parse extra arguments
            String instances = args[7];
            String[] deletions = args[8].split(", ");
            String[] changes = args[9].split(", ");
            String[] insertions = args[10].split(", ");
            int[] changeList = combineStringNumberArrays(deletions, changes, insertions);

            controller.nChanges(changeList, tempDir + "generated/", instanceFileName,
                    instances);
        }
        if (mode.equals("blank")) {
            String old_instance_fileName = tempDir + "old_" + instanceFileName;
            String new_instance_fileName = tempDir + "new_" + instanceFileName;
            String fullTemplateFileName = tempDir + templateFileName;

            // initial population of the triple store
            populateDB(log, fi, new_instance_fileName, tm, dbURL);
            log.print(LOGTAG.FUSEKI, "Initial population of the Original graph.");
            controller.testSingleFile(new_instance_fileName, old_instance_fileName, fullTemplateFileName);
        }
        try {
            timer.writeSplitsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
