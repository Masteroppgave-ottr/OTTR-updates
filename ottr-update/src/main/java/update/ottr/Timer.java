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
    private long startTime;
    private long endTime;
    private ArrayList<Split> splitList;

    public Timer(String outputFile) {
        this.outputFile = outputFile;
        this.splitList = new ArrayList<Split>();
    }

    public void startTimer() {
        startTime = System.nanoTime();
        splitList.add(new Split("start", startTime));
    }

    public void endTimer() {
        endTime = System.nanoTime();
        splitList.add(new Split("end", endTime));
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return endTime - startTime;
    }

    public ArrayList<Split> getSplits() {
        return splitList;
    }

    public ArrayList<Split> getSplits(String label) {
        ArrayList<Split> splits = new ArrayList<Split>();
        for (Split s : splitList) {
            if (s.label.equals(label)) {
                splits.add(s);
            }
        }
        return splits;
    }

    public void addSplit(String label) {
        splitList.add(new Split(label, System.nanoTime()));
    }

    public void writeSplitsToFile() throws IOException {
        FileWriter fw = new FileWriter(outputFile);
        for (Split s : splitList) {
            fw.write(s.label + " : " + s.time + "\n");
        }
        fw.close();
    }
}
