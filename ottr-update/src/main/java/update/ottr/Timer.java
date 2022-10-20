package update.ottr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Timer {
    private class Split {
        public String label;
        public long time;

        public Split(String label, long time) {
            this.label = label;
            this.time = time;
        }

        @Override
        public String toString() {
            return label + ": " + time;
        }
    }

    private String outputFile;
    private ArrayList<Split> splitList;

    public Timer(String outputFile) {
        this.outputFile = outputFile;
        this.splitList = new ArrayList<Split>();
    }

    /**
     * Get the timestamps associated with a tag
     * 
     * @param label
     * @return
     */
    private ArrayList<Long> getTimes(String label) {
        ArrayList<Long> times = new ArrayList<Long>();
        for (Split split : splitList) {
            if (split.label.equals("start")) {
                times.add(split.time);
            }
        }
        return times;
    }

    public void startTimer() {
        splitList.add(new Split("start", System.nanoTime()));
    }

    public void endTimer() {
        splitList.add(new Split("end", System.nanoTime()));
    }

    public long getStartTime() {
        return getTimes("start").get(0);
    }

    public long getEndTime() {
        return getTimes("end").get(-1);
    }

    /**
     * Returns the time between the "start" and "end" split
     * 
     * @return
     *         The duration in nano seconds
     */
    public long getDuration() {
        return getTimes("endTime").get(-1) - getTimes("startTime").get(0);
    }

    public ArrayList<Split> getSplits() {
        return splitList;
    }

    /**
     * Get all splits with a specific label
     * 
     * @param label
     *              The label to search for
     * @return
     *         An ArrayList of all splits with the given label
     */
    public ArrayList<Split> getSplits(String label) {
        ArrayList<Split> splits = new ArrayList<Split>();
        for (Split s : splitList) {
            if (s.label.equals(label)) {
                splits.add(s);
            }
        }
        return splits;
    }

    /**
     * Create a new split with a label and current time
     * 
     * @param label
     *              The label to give the split
     */
    public void newSplit(String label) {
        splitList.add(new Split(label, System.nanoTime()));
    }

    /**
     * Write the splits to a the file defined in the constructor.
     * The file will be overwritten if it already exists.
     * The file wil consist of one line per split with the format:
     * `label : time`
     * 
     * @throws IOException
     *                     If the file cannot be written to
     */
    public void writeSplitsToFile() throws IOException {
        FileWriter fw = new FileWriter(outputFile);
        for (Split s : splitList) {
            fw.write(s.label + " : " + s.time + "\n");
        }
        fw.close();
    }

    /**
     * print the splits to the console
     */
    public void writeSplitsStdout() {
        for (Split s : splitList) {
            System.out.println(s.label + " : " + s.time + "\n");
        }
    }
}
