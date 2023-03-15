package update.ottr;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

import xyz.ottr.lutra.TemplateManager;

public class Controller {
    String[] solutions;
    Logger log;
    Timer timer;
    String dbURL;
    TemplateManager tm;
    String baseDBFileName;
    LOGTAG logLevel = LOGTAG.TEST;
    FusekiInterface fuseki;

    private boolean contains(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }

    public Controller(String[] solutions, Logger log, Timer timer, String dbURL, TemplateManager tm) {
        this.solutions = solutions;
        this.log = log;
        this.timer = timer;
        this.dbURL = dbURL;
        this.tm = tm;
        this.fuseki = new FusekiInterface(log);
    }

    /**
     * Check if two graphs are isomorphic.
     * The result logged to the user
     */
    private boolean compareDataset(String datasetName1, String datasetName2) {
        boolean isIsomorphic = false;
        log.print(LOGTAG.DEBUG, "Comparing graphs " + datasetName1 + " and " + datasetName2);
        try {
            Model updated = fuseki.getDataset(dbURL, datasetName1);
            Model rebuild = fuseki.getDataset(dbURL, datasetName2);
            log.print(LOGTAG.DEBUG, "Got graphs\n" + updated + "\nand\n" + rebuild);
            isIsomorphic = updated.isIsomorphicWith(rebuild);
            if (isIsomorphic) {
                log.print(LOGTAG.DEFAULT, "Graphs are isomorphic");
            } else {
                log.print(LOGTAG.ERROR, "Graphs are not isomorphic");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isIsomorphic;
    }

    /**
     * For every n in numElements:
     * run the solutions specified in 'this.solutions'
     * log the time it took to run the solution. The times are saved to timerPath
     * 
     * @param numElements
     *                         an array of n sizes
     * @param generatedPath
     *                         path to the generated ottr-files
     * @param instanceFileName
     *                         the name of the original instance file
     */
    public void nInstances(String[] numElements, String generatedPath, String instanceFileName, String changes) {
        OttrInterface ottrInterface = new OttrInterface(log);
        FusekiInterface fuseki = new FusekiInterface(log);
        for (String n : numElements) {
            String pathToNewInstances = generatedPath + n + "_new_" + instanceFileName;
            String pathToOldInstances = generatedPath + n + "_old_" + instanceFileName;
            // check if the files exist
            if (!new java.io.File(pathToNewInstances).exists()) {
                log.print(LOGTAG.ERROR, "File " + pathToNewInstances + " does not exist");
                System.exit(0);
            }
            if (!new java.io.File(pathToOldInstances).exists()) {
                log.print(LOGTAG.ERROR, "File " + pathToOldInstances + " does not exist");
                System.exit(0);
            }

            Model baseModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
            try {
                fuseki.resetDb(baseModel, dbURL);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (this.contains(solutions, Solutions.SIMPLE + "")) {
                log.print(logLevel, "START simple update for " + n + " instances");
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(n), Integer.parseInt(changes));
                log.print(logLevel, "DONE  simple update for " + n + " instances");
            }
            if (contains(solutions, Solutions.BLANK + "")) {
                log.print(logLevel, "START blank node update for " + n + " instances");
                BlankNode blankNode = new BlankNode(log, dbURL, timer);
                blankNode.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances, tm, Integer.parseInt(n),
                        Integer.parseInt(changes));
                log.print(logLevel, "DONE  blank node update for " + n + " instances");
            }
            if (contains(solutions, Solutions.DUPLICATE + "")) {
                log.print(logLevel, "START duplicate update for " + n + " instances");
                Duplicates duplicates = new Duplicates(log, dbURL, timer, tm);

                // reset the database to the old instances with a correct counter
                try {
                    fuseki.clearDb(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Model oldModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
                duplicates.insertModel(oldModel);

                duplicates.runDuplicateUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(n),
                        Integer.parseInt(changes));
                log.print(logLevel, "DONE duplicate update for " + n + " instances");
            }
            if (contains(solutions, Solutions.REBUILD + "")) {
                log.print(logLevel, "START rebuild update for " + n + " instances");
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n, changes);
                compareDataset("Updated", "Rebuild");
                log.print(logLevel, "DONE  rebuild update for " + n + " instances");
            }

            try {
                timer.writeSplitsToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void nChanges(int[] changes, String generatedPath, String instanceFileName, String numInstances) {
        String pathToOldInstances = generatedPath + numInstances + "_old_" + instanceFileName;
        OttrInterface ottrInterface = new OttrInterface(log);
        for (int n : changes) {
            String pathToNewInstances = generatedPath + numInstances + "_changes_" + n + "_new_" + instanceFileName;
            Model newModel = ottrInterface.expandAndGetModelFromFile(pathToNewInstances, tm);
            try {
                fuseki.resetDb(newModel, dbURL);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (contains(solutions, Solutions.SIMPLE + "")) {
                log.print(logLevel, "START simple update for " + n + " changes");
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(numInstances), n);
                log.print(logLevel, "DONE  simple update for " + n + " changes");
            }
            if (contains(solutions, Solutions.BLANK + "")) {
                log.print(logLevel, "START blank node update for " + n + " changes");
                BlankNode blankNode = new BlankNode(log, dbURL, timer);
                blankNode.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances, tm,
                        Integer.parseInt(numInstances),
                        n);
                log.print(logLevel, "DONE  blank node update for " + n + " changes");
            }
            if (contains(solutions, Solutions.DUPLICATE + "")) {
                log.print(logLevel, "START duplicate update for " + n + " instances");
                Duplicates duplicates = new Duplicates(log, dbURL, timer, tm);

                // reset the database to the old instances with a correct counter
                try {
                    fuseki.clearDb(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Model oldModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
                duplicates.insertModel(oldModel);

                duplicates.runDuplicateUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(numInstances),
                        n);
                log.print(logLevel, "DONE duplicate update for " + n + " instances");
            }
            if (contains(solutions, Solutions.REBUILD + "")) {
                log.print(logLevel, "START rebuild update for " + n + " changes");
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, numInstances, n + "");
                compareDataset("Updated", "Rebuild");
                log.print(logLevel, "DONE  rebuild update for " + n + " changes");
            }
            try {
                timer.writeSplitsToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testSingleFile(int n, int changes, String pathToNewInstances, String pathToOldInstances,
            String pathToTemplates) {
        BlankNode b = new BlankNode(log, dbURL, timer);
        b.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances,
                tm, n, changes);
    }
}
