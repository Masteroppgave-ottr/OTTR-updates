package update.ottr;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;

import xyz.ottr.lutra.TemplateManager;

public class Controller {
    ArrayList<Solutions> solutions;
    Logger log;
    Timer timer;
    String dbURL;
    TemplateManager tm;
    String baseDBFileName;

    public Controller(ArrayList<Solutions> solutions, Logger log, Timer timer, String dbURL, TemplateManager tm) {
        this.solutions = solutions;
        this.log = log;
        this.timer = timer;
        this.dbURL = dbURL;
        this.tm = tm;
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
    public void nElements(String[] numElements, String generatedPath, String instanceFileName) {
        OttrInterface ottrInterface = new OttrInterface(log);
        FusekiInterface fuseki = new FusekiInterface(log);
        for (String n : numElements) {
            String pathToNewInstances = generatedPath + n + "_new_" + instanceFileName;
            String pathToOldInstances = generatedPath + n + "_old_" + instanceFileName;
            Model baseModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances, tm);
            try {
                fuseki.resetDb(baseModel, dbURL);
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (solutions.contains(Solutions.SIMPLE)) {
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(n));
            }
            if (solutions.contains(Solutions.REBUILD)) {
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n);
            }

            try {
                Model updated = fuseki.getGraph(dbURL, "Updated");
                Model rebuild = fuseki.getGraph(dbURL, "Rebuild");
                //compare the models
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

            if (solutions.contains(Solutions.SIMPLE)) {
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer, n);
            }
            if (solutions.contains(Solutions.REBUILD)) {
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n + "");
            }
        }

    }

    public void procentChanges(String[] numElements, String[] changeProcent) {
        for (String n : numElements) {

        }
    }

    public void testSingleFile(String pathToNewInstances, String pathToOldInstances, String n) {
        // test all solutions with this dataset
        if (solutions.contains(Solutions.SIMPLE)) {
            SimpleUpdate simpleUpdate = new SimpleUpdate(this.log);
            simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                    Integer.parseInt(n));
        }
    }
}
