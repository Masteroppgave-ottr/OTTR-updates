package update.ottr;

import java.io.IOException;
import java.util.Scanner;

import org.apache.jena.rdf.model.Model;

import xyz.ottr.lutra.TemplateManager;

public class Controller {
    String[] solutions;
    String solutionString;
    Logger log;
    Timer timer;
    String dbURL;
    TemplateManager tm;
    String baseDBFileName;
    LOGTAG logLevel = LOGTAG.TEST;
    FusekiInterface fuseki;
    Scanner scanner;
    OttrInterface ottrInterface;

    private boolean contains(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private void userBreakpoint() {
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public Controller(String[] solutions, Logger log, Timer timer, String dbURL, TemplateManager tm,
            OttrInterface ottrInterface) {
        this.solutions = solutions;
        this.log = log;
        this.timer = timer;
        this.dbURL = dbURL;
        this.tm = tm;
        this.fuseki = new FusekiInterface(log);
        this.scanner = new Scanner(System.in);
        this.ottrInterface = ottrInterface;
        this.solutionString = "(";
        for (String s : solutions) {
            solutionString += s + "_";
        }
        solutionString = solutionString.substring(0, solutionString.length() - 1) + ")";
    }

    /**
     * Check if two graphs are isomorphic.
     * The result logged to the user
     */
    private boolean compareDataset(String datasetName1, String datasetName2) {
        boolean isIsomorphic = false;
        try {
            Model updated = fuseki.getDataset(dbURL, datasetName1);
            Model rebuild = fuseki.getDataset(dbURL, datasetName2);
            isIsomorphic = updated.isIsomorphicWith(rebuild);
            if (isIsomorphic) {
                log.print(LOGTAG.SUCCESS, "Graphs are isomorphic");
            } else {
                log.print(LOGTAG.ERROR, "Graphs are not isomorphic");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isIsomorphic;
    }

    private void warmup(int instances, String generatedPath, String fileName, int warmupSeconds) {
        long before = System.currentTimeMillis();
        Rebuild rebuild = new Rebuild();
        Duplicates duplicates = new Duplicates(log, dbURL, timer, ottrInterface);
        BlankNode blankNode = new BlankNode(log, dbURL, timer, ottrInterface);

        log.print(LOGTAG.TEST, "START warmup");
        while (((System.currentTimeMillis() - before) / 1000) < warmupSeconds) {
            String newF = generatedPath + instances + "_new_" + fileName;
            String oldF = generatedPath + instances + "_old_" + fileName;

            rebuild.buildRebuildSet(newF, tm, log, timer, dbURL,
                    null, "0");
            duplicates.runDuplicateUpdate(oldF, newF, -1, 0);
            blankNode.runBlankNodeUpdate(oldF, newF, -1, 0);
        }
        log.print(LOGTAG.TEST, "DONE  warmup in " + ((System.currentTimeMillis() - before) / 1000) + " seconds");

        // sleep for 1 second
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    public void nInstances(String[] numElements, String generatedPath, String instanceFileName, String changes,
            String deletions, String explicitChanges, String insertions, int warmupSeconds) {
        FusekiInterface fuseki = new FusekiInterface(log);

        Rebuild rebuild = new Rebuild();
        Duplicates duplicates = new Duplicates(log, dbURL, timer, ottrInterface);
        BlankNode blankNode = new BlankNode(log, dbURL, timer, ottrInterface);
        Combined combined = new Combined(log, dbURL, timer, ottrInterface);

        warmup(Integer.parseInt(numElements[0]), generatedPath, instanceFileName, warmupSeconds);

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

            Model baseModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances);

            if (contains(solutions, Solutions.REBUILD + "")) {
                log.print(logLevel, "START rebuild update for " + n + " instances");
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n, changes);
                log.print(logLevel, "DONE  rebuild update for " + n + " instances");
            }
            if (this.contains(solutions, Solutions.SIMPLE + "")) {
                try {
                    fuseki.resetUpdatedDataset(baseModel, dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                log.print(logLevel, "START simple update for " + n + " instances");
                SimpleUpdate simpleUpdate = new SimpleUpdate(log, tm);
                simpleUpdate.runSimpleUpdate(log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(n), Integer.parseInt(changes));
                log.print(logLevel, "DONE  simple update for " + n + " instances");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            if (contains(solutions, Solutions.BLANK + "")) {
                try {
                    fuseki.resetUpdatedDataset(baseModel, dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                log.print(logLevel, "START blank node update for " + n + " instances");
                blankNode.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(n),
                        Integer.parseInt(changes));
                log.print(logLevel, "DONE  blank node update for " + n + " instances");
                // if (contains(solutions, Solutions.REBUILD + "")) {
                // compareDataset("Updated", "Rebuild");
                // }
            }
            if (contains(solutions, Solutions.DUPLICATE + "")) {
                try {
                    fuseki.clearUpdated(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // reset the database to the old instances with a correct counter
                duplicates.insertFromFile(pathToOldInstances);
                log.print(logLevel, "START duplicate update for " + n + " instances");

                duplicates.runDuplicateUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(n),
                        Integer.parseInt(changes));
                log.print(logLevel, "DONE duplicate update for " + n + " instances");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            if (contains(solutions, Solutions.COMBINED + "")) {
                try {
                    fuseki.clearUpdated(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // reset the database to the old instances with a correct counter
                combined.insertFromFile(pathToOldInstances);
                log.print(logLevel, "START combined update for " + n + " instances");

                combined.runCombinedUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(n),
                        Integer.parseInt(changes));
                log.print(logLevel, "DONE combined update for " + n + " instances");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
        }
        try {
            timer.writeSplitsToFile(
                    "nInstances_instances-" + numElements[0] + "-" + numElements[numElements.length - 1] + "_"
                            + "deletions-" + deletions + "_insertions-" + insertions + "_" + solutionString
                            + ".txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nChanges(int[] changes, String generatedPath, String instanceFileName, String numInstances,
            String[] deletions, String[] insertions, int warmupSeconds) {
        String pathToOldInstances = generatedPath + numInstances + "_old_" + instanceFileName;

        warmup(Integer.parseInt(numInstances), generatedPath, instanceFileName, warmupSeconds);
        Model baseModel = ottrInterface.expandAndGetModelFromFile(pathToOldInstances);

        for (int n : changes) {
            String pathToNewInstances = generatedPath + numInstances + "_changes_" + n + "_new_" + instanceFileName;
            Model newModel = ottrInterface.expandAndGetModelFromFile(pathToNewInstances);

            if (contains(solutions, Solutions.REBUILD + "")) {
                log.print(logLevel, "START rebuild update for " + n + " changes");
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, numInstances, n + "");
                log.print(logLevel, "DONE  rebuild update for " + n + " changes");
            }

            if (contains(solutions, Solutions.SIMPLE + "")) {
                try {
                    fuseki.resetUpdatedDataset(baseModel, dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.print(logLevel, "START simple update for " + n + " changes");
                SimpleUpdate simpleUpdate = new SimpleUpdate(log, tm);
                simpleUpdate.runSimpleUpdate(log, pathToNewInstances, pathToOldInstances, dbURL, timer,
                        Integer.parseInt(numInstances), n);
                log.print(logLevel, "DONE  simple update for " + n + " changes");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            if (contains(solutions, Solutions.BLANK + "")) {
                try {
                    fuseki.resetUpdatedDataset(baseModel, dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.print(logLevel, "START blank node update for " + n + " changes");
                BlankNode blankNode = new BlankNode(log, dbURL, timer, ottrInterface);
                blankNode.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(numInstances), n);
                log.print(logLevel, "DONE  blank node update for " + n + " changes");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            if (contains(solutions, Solutions.DUPLICATE + "")) {
                log.print(logLevel, "START duplicate update for " + n + " instances");
                Duplicates duplicates = new Duplicates(log, dbURL, timer, ottrInterface);

                // reset the database to the old instances with a correct counter
                try {
                    fuseki.clearUpdated(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                duplicates.insertFromFile(pathToOldInstances);

                duplicates.runDuplicateUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(numInstances),
                        n);
                log.print(logLevel, "DONE duplicate update for " + n + " instances");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            if (contains(solutions, Solutions.COMBINED + "")) {
                log.print(logLevel, "START combined update for " + n + " instances");
                Combined combined = new Combined(log, dbURL, timer, ottrInterface);

                // reset the database to the old instances with a correct counter
                try {
                    fuseki.clearUpdated(dbURL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                combined.insertFromFile(pathToOldInstances);

                combined.runCombinedUpdate(pathToOldInstances, pathToNewInstances, Integer.parseInt(numInstances),
                        n);
                log.print(logLevel, "DONE combined update for " + n + " instances");
                if (contains(solutions, Solutions.REBUILD + "")) {
                    compareDataset("Updated", "Rebuild");
                }
            }
            try {
                timer.writeSplitsToFile("nChanges_instances-" + numInstances + "_deletions-" + deletions[0] + "-"
                        + deletions[deletions.length - 1] + "_insertions" + insertions[0] + "-"
                        + insertions[insertions.length - 1] + "_" + solutionString + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testSingleFile(int n, int changes, String pathToNewInstances, String pathToOldInstances,
            String pathToTemplates) {
        BlankNode b = new BlankNode(log, dbURL, timer, ottrInterface);
        b.runBlankNodeUpdate(pathToOldInstances, pathToNewInstances,
                n, changes);
    }
}
