package update.ottr;

import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.ottr.lutra.api.StandardFormat;
import xyz.ottr.lutra.api.StandardTemplateManager;
import xyz.ottr.lutra.io.Format;
import xyz.ottr.lutra.io.Format.Support;
import xyz.ottr.lutra.store.TemplateStore;
import xyz.ottr.lutra.stottr.StottrFormat;
import xyz.ottr.lutra.system.ResultStream;

public class App 
{
    public static void main( String[] args )
    //run this in ottr-update folder:
    //java -cp target/ottr-update-1.0-SNAPSHOT.jar:../lutra.jar update.ottr.App
    {
        StandardTemplateManager stm = new StandardTemplateManager();
        // TemplateStore tStore = stm.getStandardLibrary();
        Format format = new StottrFormat();
        ResultStream resultStream = stm.readInstances(format, "home/ottr/ottr-update/src/main/resources/instances");
        resultStream.flatMap(Function.identity());
    }
}
