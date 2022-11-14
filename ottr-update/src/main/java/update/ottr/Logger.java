package update.ottr;

import java.util.ArrayList;

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

    public Logger(ArrayList<LOGTAG> activeTags) {
        if (activeTags == null) {
            this.activeTags = new ArrayList<LOGTAG>();
        } else {
            this.activeTags = activeTags;
        }
        this.disabled = false;
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
                System.out.println(ANSI_RED + "["+tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.WARNING) {
                System.out.println(ANSI_YELLOW + "["+tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.DEBUG) {
                System.out.println(ANSI_BLUE + "["+tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.FUSEKI) {
                System.out.println(ANSI_PURPLE + "["+tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.OTTR) {
                System.out.println(ANSI_CYAN + "["+tag + "]" + ANSI_RESET + message);
            } else if (tag == LOGTAG.DIFF) {
                System.out.println(ANSI_GREEN + "["+tag + "]" + ANSI_RESET + message);
            } else {
                System.out.println("[" + tag + "] " + message);
            }
        }
    }
}
