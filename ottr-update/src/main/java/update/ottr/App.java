package update.ottr;

import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;
import org.apache.jena.sparql.function.library.leviathan.log;

import xyz.ottr.lutra.system.Message;
import xyz.ottr.lutra.model.Signature;
import xyz.ottr.lutra.stottr.io.SFileReader;
import xyz.ottr.lutra.stottr.parser.SInstanceParser;
import xyz.ottr.lutra.stottr.parser.STemplateParser;
import xyz.ottr.lutra.system.Assertions;
import xyz.ottr.lutra.system.ResultConsumer;
import xyz.ottr.lutra.system.ResultStream;

public class App 
{
    public static void main( String[] args )
    {
        SFileReader reader = new SFileReader();
        
        /* parse templates */
        ResultStream<CharStream> templateStream = reader.apply("/home/magnus/Emner/Master/dev/temp/templates.stottr");
        // templateStream.forEach(r -> {
        //     System.out.println(r.get().toString() + "\n");
        // });

        String templateStr = templateStream.getStream().findFirst().get().get().toString();
        STemplateParser sParser = new STemplateParser();
        ResultConsumer<Signature> consumer = new ResultConsumer<>();            
        sParser.parseString(templateStr).forEach(result -> System.out.println("Parsed :" + result));
        
        
        /* parse instances */
        ResultStream<CharStream> instanceStream = reader.apply("/home/magnus/Emner/Master/dev/temp/instances.stottr");        
    }
}
