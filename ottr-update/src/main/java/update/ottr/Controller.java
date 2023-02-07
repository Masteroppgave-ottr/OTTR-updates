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
    LOGTAG logLevel = LOGTAG.BLANK;

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
    }

    private boolean fileExists(String path) {
        return new java.io.File(path).exists();
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
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(n), Integer.parseInt(changes));
            }
            if (contains(solutions, Solutions.REBUILD + "")) {
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n, changes);
            }

            if (contains(solutions, Solutions.REBUILD + "")) {
                try {

                    Model updated = fuseki.getGraph(dbURL, "Updated");
                    Model rebuild = fuseki.getGraph(dbURL, "Rebuild");
                    // compare the models
                    if (updated.isIsomorphicWith(rebuild)) {
                        log.print(LOGTAG.DEFAULT, "The models are isomorphic");
                    } else {
                        log.print(LOGTAG.ERROR, "The models are not isomorphic");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void nChanges(int[] changes, String generatedPath, String instanceFileName, String numInstances) {
        String pathToOldInstances = generatedPath + numInstances + "_old_" + instanceFileName;
        OttrInterface ottrInterface = new OttrInterface(log);
        Model baseModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
        FusekiInterface fuseki = new FusekiInterface(log);
        for (int n : changes) {
            try {
                fuseki.resetDb(baseModel, dbURL);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String pathToNewInstances = generatedPath + numInstances + "_changes_" + n + "_new_" + instanceFileName;

            if (contains(solutions, Solutions.SIMPLE + "")) {
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(numInstances), n);
            }
            if (contains(solutions, Solutions.REBUILD + "")) {
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, numInstances, n + "");
            }
        }

    }

    public void testSingleFile(String pathToNewInstances, String pathToOldInstances, String pathToTemplates) {
        BlankNode b = new BlankNode(log);
        b.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances, pathToTemplates);
    }
}
