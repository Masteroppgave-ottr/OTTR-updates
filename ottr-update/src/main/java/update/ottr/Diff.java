package update.ottr;

import java.io.*;
import java.util.ArrayList;

class Diff {

    public ArrayList<Integer> addLines;
    public ArrayList<Integer> deleteLines;
    public char ignoreCharacters[];
    private Logger log;
    private LOGTAG logLevel = LOGTAG.DIFF;

    /**
     * Class used to compare two files containing OTTR instances.
     * 
     * Args:
     * oldFile (String): Path to the old instance file containing the original
     * instances before modification.
     * newFile (String): Path to the new instance file containing the modified
     * instances.
     */
    public Diff(Logger log) {
        this.addLines = new ArrayList<Integer>();
        this.deleteLines = new ArrayList<Integer>();
        this.ignoreCharacters = new char[] { '<', '>', '\\', '-' };
        this.log = log;
    }

    /**
     * check if a character `c` is in the `characters` array
     */
    private boolean contains(char[] characters, char c) {
        for (char x : characters) {
            if (x == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * return the first letter (not number or ignoreCharacter) in a string
     */
    private char firstLetter(String s) {
        if (contains(ignoreCharacters, s.charAt(0)))
            return '\0';

        int i = 0;
        while (i < s.length() && !Character.isAlphabetic(s.charAt(i))) {
            i++;
        }
        return s.charAt(i);
    }

    /**
     * given a diff code `s` return an array of the one or two numbers before the
     * letter. If there only is one number the second number is -1.
     * Example:"1,2a2"->[1,2]"1a2"->[1,-1]*
     */
    private int[] getNumbersBefore(String s) {
        int[] numbers = new int[] { -1, -1 };
        int firstNumEnd = 0;

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                numbers[0] = Integer.parseInt(s.substring(0, i));
                firstNumEnd = i;
                break;
            }
        }

        if (s.charAt(firstNumEnd) == ',') {
            for (int i = firstNumEnd + 1; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    numbers[1] = Integer.parseInt(s.substring(firstNumEnd + 1, i));
                    break;
                }
            }
        }

        return numbers;
    }

    /**
     * given a diff code `s` return an array of the one or two numbers after the
     * letter. If there only is one number the second number is -1.
     * Example:"1a2,4"->[2,4]"1a2"->[2,-1]*
     */
    private int[] getNumbersAfter(String s) {
        int[] numbers = new int[] { -1, -1 };

        // find the letter in the middle
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)) && s.charAt(i) != ',') {
                start = i + 1;
                break;
            }
        }

        // find the comma after the letter
        int firstNumEnd = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                firstNumEnd = i;
                break;
            }
        }

        // get numbers
        if (firstNumEnd == 0) {
            numbers[0] = Integer.parseInt(s.substring(start, s.length()));
        } else {
            numbers[0] = Integer.parseInt(s.substring(start, firstNumEnd));
            numbers[1] = Integer.parseInt(s.substring(firstNumEnd + 1, s.length()));
        }

        return numbers;
    }

    private void handleAdd(String s) {
        int numbers[] = getNumbersAfter(s);
        if (numbers[1] == -1) {
            addLines.add(numbers[0]);
        } else {
            for (int i = numbers[0]; i <= numbers[1]; i++) {
                addLines.add(i);
            }
        }
    }

    private void handleDelete(String s) {
        int numbers[] = getNumbersBefore(s);
        // there is only one line number
        if (numbers[1] == -1) {
            deleteLines.add(numbers[0]);
        } else {
            for (int i = numbers[0]; i <= numbers[1]; i++) {
                deleteLines.add(i);
            }
        }
    }

    private void handleChange(String s) {
        handleDelete(s);
        handleAdd(s);
    }

    /**
     * after reading a diff with `readDiffFromStdIn()` the results can be written to
     * two files_
     * addFile: contains all the line number of the added instances.
     * This is the new file with updated instances.
     * deleteFile: contains all the line number of the deleted instances.
     * This is the old file with the original instances.
     */
    public void writeLineNumbersToFile(String addFile, String removeFile) {
        try {
            FileWriter addFW = new FileWriter(addFile);
            FileWriter deleteFW = new FileWriter(removeFile);

            for (Integer i : addLines) {
                addFW.write(i + "\n");
            }

            for (Integer i : deleteLines) {
                deleteFW.write(i + "\n");
            }

            addFW.close();
            deleteFW.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private String getInstancesString(String instanceFileName, ArrayList<Integer> lineNumberList)
            throws FileNotFoundException {
        String s = "";
        int linesAdded = 0;
        if (lineNumberList.size() == 0)
            return null;

        System.out.println("Looking for line " + lineNumberList);
        System.out.println("in file " + instanceFileName);

        try {
            BufferedReader br = new BufferedReader(new FileReader(instanceFileName));

            int currentLine = 1;
            int addIndex = 0;
            int getLine = lineNumberList.get(addIndex);
            while (br.ready()) {
                // read through file until the correct line
                for (; currentLine < getLine; currentLine++) {
                    br.readLine();
                }

                // found the correct line. Add to s and find next line
                s += br.readLine() + "\n";
                linesAdded++;
                currentLine++;
                addIndex++;
                if (addIndex < lineNumberList.size()) {
                    getLine = lineNumberList.get(addIndex);
                } else {
                    break;
                }
            }

            br.close();
        } catch (IOException e) {
            System.out.println("Error while reading the file");
            e.printStackTrace();
        }

        // If there is a different number of lines in the add list and the created
        // string something is wrong.
        if (linesAdded != lineNumberList.size()) {
            throw new RuntimeException("Could not find all the lines in the instance file");
        }

        return s;
    }

    /**
     * From addLines write the corresponding content at the line number to a string
     * 
     * @param instanceFileName
     *                         the file to read the instances from. This is the new
     *                         file with updated instances
     * @return
     *         A string containing all the content from the line numbers in addLines
     * 
     * @throws FileNotFoundException
     */
    public String getAddInstancesString(String newInstanceFileName) throws FileNotFoundException {
        return getInstancesString(newInstanceFileName, addLines);
    }

    /**
     * From deleteLine write the corresponding content at the line number to a
     * string
     * 
     * @param instanceFileName
     *                         the file to read the instances from. This is the old
     *                         file with outdated instances
     * @return
     *         A string containing all the content from the line numbers in
     *         deleteLines
     * 
     * @throws FileNotFoundException
     */
    public String getDeleteInstancesString(String oldInstanceFileName) throws FileNotFoundException {
        return getInstancesString(oldInstanceFileName, deleteLines);
    }

    /**
     * Read a unix diff from stdin and write the affected line numbers to 2 lists:
     * addLines,
     * deleteLines,
     */
    public void readDiffFromInputStream(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = "<";

        while (line != null) {
            try {
                switch (firstLetter(line)) {
                    case 'a':
                        handleAdd(line);
                        break;
                    case 'c':
                        handleChange(line);
                        break;
                    case 'd':
                        handleDelete(line);
                        break;
                    default:
                        break;
                }

                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        log.print(logLevel, "lines to add: " + addLines.toString());
        log.print(logLevel, "lines to remove: " + deleteLines.toString());
    }

    public void readDiffFromStdIn() {
        readDiffFromInputStream(System.in);
    }

    public void readDiff(String file1, String file2) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("diff", file1, file2);
        try {
            Process process = processBuilder.start();
            readDiffFromInputStream(process.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}