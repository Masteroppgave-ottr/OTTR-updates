package update.ottr;

import java.io.*;
import java.util.ArrayList;

class Diff {

    ArrayList<String> delete;
    ArrayList<String> add;
    ArrayList<String> change;
    char ignoreCharacters[];

    /*
     * Class used to compare two files containing OTTR instances.
     * 
     * Args:
     * oldFile (String): Path to the old instance file containing the original
     * instances before modification.
     * newFile (String): Path to the new instance file containing the modified
     * instances.
     */
    public Diff() {
        this.delete = new ArrayList<String>();
        this.add = new ArrayList<String>();
        this.change = new ArrayList<String>();
        this.ignoreCharacters = new char[] { '<', '>', '\\', '-' };
    }

    /*
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

    /*
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

    /*
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

    /*
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

    /*
     * after reading a diff the results can be written to two files_
     * addFile: contains all the line number of the added instances.
     * This is the new file with updated instances.
     * deleteFile: contains all the line number of the deleted instances.
     * This is the old file with the original instances.
     */
    void writeToFile(String addFile, String removeFile) {
        try {
            FileWriter addFW = new FileWriter(addFile);
            FileWriter deleteFW = new FileWriter(removeFile);

            for (String s : add) {
                int numbers[] = getNumbersAfter(s);
                if (numbers[1] == -1) {
                    addFW.write(numbers[0] + "\n");
                } else {
                    for (int i = numbers[0]; i <= numbers[1]; i++) {
                        addFW.write(i + "\n");
                    }
                }
            }

            for (String s : delete) {
                int numbers[] = getNumbersBefore(s);
                // there is only one line number
                if (numbers[1] == -1) {
                    deleteFW.write(numbers[0] + "\n");
                } else {
                    for (int i = numbers[0]; i <= numbers[1]; i++) {
                        deleteFW.write(i + "\n");
                    }
                }
            }

            for (String s : change) {
                int numbers[] = getNumbersBefore(s);
                // there is only one line number
                if (numbers[1] == -1) {
                    deleteFW.write(numbers[0] + "\n");
                } else {
                    for (int i = numbers[0]; i <= numbers[1]; i++) {
                        deleteFW.write(i + "\n");
                    }
                }

                numbers = getNumbersAfter(s);
                if (numbers[1] == -1) {
                    addFW.write(numbers[0] + "\n");
                } else {
                    for (int i = numbers[0]; i <= numbers[1]; i++) {
                        addFW.write(i + "\n");
                    }
                }
            }

            addFW.close();
            deleteFW.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /*
     * Read a unix diff from stdin and write the edit codes to 3 lists: add, delete,
     * change
     */
    public void readDiffFromStdIn() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = "<";

        while (line != null) {
            try {
                // switch case to check for the first character of the line
                switch (firstLetter(line)) {
                    case 'a':
                        add.add(line);
                        break;
                    case 'c':
                        change.add(line);
                        break;
                    case 'd':
                        delete.add(line);
                        break;
                    default:
                        break;
                }

                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Add:");
        System.out.println(add);
        System.out.println("Delete:");
        System.out.println(delete);
        System.out.println("Change:");
        System.out.println(change);
    }
}