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

    public void print(String tag, String message) {
        if (!disabled && activeTags.contains(tag)) {
            System.out.println("[" + tag + "] " + message);
            existingTags.add(tag);
        }
    }

    public Set<String> getExistingTags() {
        return existingTags;
    }
}
