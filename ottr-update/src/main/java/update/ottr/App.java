package update.ottr;

import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;

import xyz.ottr.lutra.stottr.io.SFileReader;
import xyz.ottr.lutra.system.Result;
import xyz.ottr.lutra.system.ResultStream;

public class App 
{
    public static void main( String[] args )
    {
        SFileReader reader = new SFileReader();
        ResultStream<CharStream> res = reader.apply("/home/magnus/Emner/Master/dev/temp/templates.stottr");

        //print content in res
        Stream<Result<CharStream>> stream = res.getStream();
        
        stream.forEach(r -> {
            System.out.println(r.get().toString());
        });

    }
}
