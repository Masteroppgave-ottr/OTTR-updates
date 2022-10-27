package update.ottr;

import java.util.ArrayList;

import xyz.ottr.lutra.TemplateManager;

public class Controller {
    ArrayList<Solutions> solutions;
    Logger log;
    Timer timer;
    String dbURL;
    TemplateManager tm;

    public Controller(ArrayList<Solutions> solutions, Logger log, Timer timer, String dbURL, TemplateManager tm) {
        this.solutions = solutions;
        this.log = log;
        this.timer = timer;
        this.dbURL = dbURL;
        this.tm = tm;
    }

    public void nElements(String[] numElements, String[] changes, String source){
        for (String n : numElements) {
            String pathToNewInstances = "../temp/generated/" + n + "_new_" + source;
            String pathToOldInstances = "../temp/generated/" + n + "_old_" + source;

            if (solutions.contains(Solutions.SIMPLE)) {
                SimpleUpdate simpleUpdate = new SimpleUpdate(log);
                simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer, Integer.parseInt(n));
            }
            if (solutions.contains(Solutions.REBUILD)) {
                Rebuild rebuild = new Rebuild();
                rebuild.buildRebuildSet(pathToNewInstances, tm, log, timer, dbURL, n);
            }
        }
    }

    public void nChanges(String[] changes, String[] numElements){
        for (String n : changes) {
            
        }
    }

    public void procentChanges(String[] numElements, String[] changeProcent){
        for (String n : numElements) {
            
        }
    }

    public void testSingleFile(String pathToNewInstances, String pathToOldInstances, String n){
        //test all solutions with this dataset
        if (solutions.contains(Solutions.SIMPLE)) {
            SimpleUpdate simpleUpdate = new SimpleUpdate(this.log);
            simpleUpdate.runSimpleUpdate(tm, log, pathToNewInstances, pathToOldInstances, dbURL, timer, Integer.parseInt(n));
        }
    }

}
