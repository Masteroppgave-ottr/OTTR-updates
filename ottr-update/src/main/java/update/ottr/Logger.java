package update.ottr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Logger {
    public ArrayList<String> activeTags;
    private Set<String> existingTags;
    private boolean disabled;

    public Logger(ArrayList<String> activeTags) {
        if (activeTags == null) {
            this.activeTags = new ArrayList<String>();
        } else {
            this.activeTags = activeTags;
        }
        this.existingTags = new HashSet<>();
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
    public void print(String tag, String message) {
        if (!disabled && activeTags.contains(tag)) {
            System.out.println("[" + tag + "] " + message);
            existingTags.add(tag);
        }
    }

    /**
     * Get a set of all tags that have been used prior to this call. This can be
     * useful to see what tags are available.
     */
    public Set<String> getExistingTags() {
        return existingTags;
    }
}
