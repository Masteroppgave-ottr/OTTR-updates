package update.ottr;

import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.jena.rdf.model.Model;
import xyz.ottr.lutra.TemplateManager;

public class Rebuild {
    private LOGTAG logLevel = LOGTAG.REBUILD;

    public void buildRebuildSet(String pathToNewInstances, TemplateManager tm, Logger log,
            Timer timer, String dbURL, String instances, String changes) {
        OttrInterface ottrInterface = new OttrInterface(log, tm);
        FusekiInterface fi = new FusekiInterface(log);

        timer.newSplit("start", "rebuild set", Integer.parseInt(instances), Integer.parseInt(changes));

        Model model = ottrInterface.expandAndGetModelFromFile(pathToNewInstances);
        log.print(logLevel, "Instances expanded\n");
        try {
            fi.rebuild(model, dbURL);
            log.print(logLevel, "Triple store updated\n");
            timer.newSplit("end", "rebuild set", Integer.parseInt(instances), Integer.parseInt(changes));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
