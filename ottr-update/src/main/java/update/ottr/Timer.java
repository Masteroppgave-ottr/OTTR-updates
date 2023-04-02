package update.ottr;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Timer {
    private class Split {
        public String solutionName;
        public int instances;
        public int changes;
        public String label;
        public long time;

        public Split(String label, String solutionName, int instances, int changes, long time) {
            this.label = label;
            this.solutionName = solutionName;
            this.instances = instances;
            this.changes = changes;
            this.time = time;
        }

        public Split(String label) {
            this.label = label;
            this.solutionName = "?";
            this.instances = 0;
            this.changes = 0;
            this.time = System.nanoTime();
        }

        @Override
        public String toString() {
            return instances + " ; " + changes + " ; " + solutionName + " ; " + label + " ; " + time;
        }

        public int compareTo(Split s) {
            if (this.instances < s.instances) {
                return -1;
            } else if (this.instances > s.instances) {
                return 1;
            } else {
                return 0;
            }
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
            if (split.label.equals(label)) {
                times.add(split.time);
            }
        }
        return times;
    }

    /**
     * Get the time of the Split with tag "start". If there is no such Split,
     * return
     * the first
     * split.
     */
    public long getStartTime() {
        // there are not splits
        if (splitList.size() < 1) {
            return -1;
        }

        ArrayList<Long> times = getTimes("start");
        if (times.size() > 0) {
            return times.get(0);
        } else {
            // if there is no time with "start" label, return the first time
            return splitList.get(0).time;
        }
    }

    /**
     * Get the time of the Split with tag "end". If there is no such Split,
     * return
     * the last time.
     */
    public long getEndTime() {
        // there are not splits
        if (splitList.size() < 1) {
            return -1;

        }

        ArrayList<Long> times = getTimes("end");
        if (times.size() > 0) {
            return times.get(0);
        } else {
            // if there is no time with "end" label, return the last time
            return splitList.get(splitList.size() - 1).time;
        }
    }

    /**
     * Returns the time between the "start" and "end" split
     * 
     * @return
     *         The duration in nano seconds
     */
    public long getDuration() {
        if (getEndTime() == -1 || getStartTime() == -1) {
            return -1;
        }

        return getEndTime() - getStartTime();
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
    public void newSplit(Split split) {
        splitList.add(split);
    }

    /**
     * Create a new split
     * 
     * @param label
     *                     The label to give the split
     * @param solutionName
     *                     The name of the solution
     * @param n
     *                     Number of instances
     */
    public void newSplit(String label, String solutionName, int instances, int changes) {
        splitList.add(new Split(label, solutionName, instances, changes, System.nanoTime()));
    }

    /**
     * Create a new split
     * 
     * @param label
     *              The label to give the split
     */
    public void newSplit(String label) {
        splitList.add(new Split(label));
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
    public void writeSplitsToFile(String copyName) throws IOException {
        // sort the splits
        System.out.println("Writing splits to file: " + outputFile);

        // sort by number of instances
        splitList.sort((s1, s2) -> s1.compareTo(s2));

        FileWriter fw = new FileWriter(outputFile);
        for (Split s : splitList) {
            fw.write(s.toString() + "\n");
        }
        fw.close();

        // fin the path
        String path = outputFile.substring(0, outputFile.lastIndexOf("/") + 1);

        // copy the file to the copyName
        if (copyName != null) {
            FileWriter fw2 = new FileWriter(path + "results/" + copyName);
            for (Split s : splitList) {
                fw2.write(s.toString() + "\n");
            }
            fw2.close();
        }
    }

    /**
     * print the splits to the console
     */
    public void writeSplitsStdout() {
        for (Split s : splitList) {
            System.out.println(s.toString() + "\n");
        }
    }
}
