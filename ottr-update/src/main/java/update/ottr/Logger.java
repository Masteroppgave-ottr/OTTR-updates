package update.ottr;

import java.util.ArrayList;

public class Logger {
    public ArrayList<LOGTAG> activeTags;
    private boolean disabled;

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
            System.out.println("[" + tag + "] " + message);
        }
    }
}
