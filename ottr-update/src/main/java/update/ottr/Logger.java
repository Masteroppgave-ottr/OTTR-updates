package update.ottr;

import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

public class Logger {
    public ArrayList<LOGTAG> activeTags;
    public boolean disabled;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_ORANGE = "\u001B[38;5;208m";
    public static final String ANSI_TURQUOISE = "\u001B[38;5;51m";
    public static final String ANSI_GREY = "\u001B[38;5;244m";
    public static final String ANSI_PINK = "\u001B[38;5;198m";

    public Logger(ArrayList<LOGTAG> activeTags) {
        if (activeTags == null) {
            this.activeTags = new ArrayList<LOGTAG>();
        } else {
            this.activeTags = activeTags;
        }
        this.disabled = false;
    }

    public void printModel(LOGTAG tag, Model model) {
        // print the triples in the model each triple on a new line
        for (Statement line : model.listStatements().toList()) {
            print(tag, line.toString());
        }
    }

    /**
     * Logs a string if the tag is active in the Logger object
     * 
     * @param tag
     *                The tag of the message
     * @param message
     *                The string to console log
     */
    public void print(LOGTAG tag, String message) {
        if (!disabled && activeTags.contains(tag)) {
            if (tag == LOGTAG.ERROR) {
                System.out.println(ANSI_RED + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.WARNING) {
                System.out.println(ANSI_YELLOW + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.DEBUG) {
                System.out.println(ANSI_BLUE + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.FUSEKI) {
                System.out.println(ANSI_PURPLE + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.OTTR) {
                System.out.println(ANSI_CYAN + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.DIFF) {
                System.out.println(ANSI_PINK + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.BLANK || tag == LOGTAG.SIMPLE || tag == LOGTAG.REBUILD) {
                System.out.println(ANSI_ORANGE + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.DUPLICATE) {
                System.out.println(ANSI_TURQUOISE + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.TEST) {
                System.out.println(ANSI_GREY + "[" + tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.SUCCESS) {
                System.out.println(ANSI_GREEN + "[" + tag + "]" + ANSI_RESET + message);
            } else {
                System.out.println("[" + tag + "] " + message);
            }
        }
    }
}
