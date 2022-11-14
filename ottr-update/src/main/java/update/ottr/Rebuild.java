package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jena.rdf.model.Model;

import xyz.ottr.lutra.TemplateManager;

public class Rebuild {
    public void buildRebuildSet(String pathToNewInstances, TemplateManager tm, Logger log,
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
}
