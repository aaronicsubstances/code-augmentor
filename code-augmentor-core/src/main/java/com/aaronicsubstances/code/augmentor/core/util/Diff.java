package com.aaronicsubstances.code.augmentor.core.util;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** 
 * Used to implement Unix diff for "normal" format, ie neither context format nor
 * unified format.
 * <p>
 * The following resources were used for implementation:
 * <ul>
 *   <li>https://introcs.cs.princeton.edu/java/23recursion/ ,
 *   <li>https://introcs.cs.princeton.edu/java/23recursion/Diff.java.html ,
 *   <li>https://introcs.cs.princeton.edu/java/23recursion/LongestCommonSubsequence.java.html
 *   <li>https://www.gnu.org/software/diffutils/manual/html_node/Detailed-Normal.html
 * </ul>
 */
public class Diff {
    
    /**
     * Generates normal diff output in normal format. Aims to mimick Unix diff command exactly.
     * @param x lines in original file. Each line should include its terminator. 
     * @param y lines in revised file. Each line should include its terminator.
     * @param writer destination of diff
     * @throws IOException
     */
    public static void printNormalDiff(List<String> x, List<String> y, Writer writer) throws IOException {
        // number of lines of each file
        int m = x.size();
        int n = y.size();

        // opt[i][j] = length of LCS of x[i..m] and y[j..n]
        int[][] opt = new int[m+1][n+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = m-1; i >= 0; i--) {
            for (int j = n-1; j >= 0; j--) {
                if (x.get(i).equals(y.get(j)))
                    opt[i][j] = opt[i+1][j+1] + 1;
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        int i = 0, j = 0;
        List<String> xLines = new ArrayList<>();
        List<String> yLines = new ArrayList<>();
        int[] xLineRange = new int[]{0, 0};
        int[] yLineRange = new int[]{0, 0};
        while (i < m && j < n) {
            if (x.get(i).equals(y.get(j))) {
                printDiffLines(writer, xLines, yLines, xLineRange, yLineRange);
                xLines.clear();
                yLines.clear();
                
                xLineRange[0] = ++i;
                yLineRange[0] = ++j;
            }
            else if (opt[i+1][j] >= opt[i][j+1]) {
                xLineRange[1] = i;
                xLines.add(x.get(i++));
            }
            else {
                yLineRange[1] = j;
                yLines.add(y.get(j++));
            }
        }
        
        while (i < m) {
            xLineRange[1] = i;
            xLines.add(x.get(i++));
        }
        
        while (j < n) {
            yLineRange[1] = j;
            yLines.add(y.get(j++));
        }
        
        printDiffLines(writer, xLines, yLines, xLineRange, yLineRange);
        writer.flush();
    }
    
    private static void printDiffLines(Writer writer, List<String> xLines, List<String> yLines,
            int[] xLineRange, int[] yLineRange) throws IOException {
        if (xLines.isEmpty() && yLines.isEmpty()) {
            return;
        }
        if (xLines.isEmpty()) {
            // insertion
            writer.write(String.format("%da%s%n", xLineRange[0], stringifyRange(yLineRange)));
            for (String y : yLines) {
                writer.write("> ");
                formatLine(y, writer);
            }
        }
        else if (yLines.isEmpty()) {
            // deletion
            writer.write(String.format("%sd%d%n", stringifyRange(xLineRange), yLineRange[0]));
            for (String x : xLines) {
                writer.write("< ");
                formatLine(x, writer);
            }
        }
        else {
            // change.
            writer.write(String.format("%sc%s%n", stringifyRange(xLineRange), stringifyRange(yLineRange)));
            for (String x : xLines) {
                writer.write("< ");
                formatLine(x, writer);
            }
            writer.write(String.format("---%n"));
            for (String y : yLines) {
                writer.write("> ");
                formatLine(y, writer);
            }
        }
    }
    
    private static String stringifyRange(int[] range) {
        if (range[0] == range[1]) {
            return "" + (range[0] + 1);
        }
        else {
            return String.format("%d,%d", range[0] + 1, range[1] + 1);
        }
    }
    
    private static void formatLine(String line, Writer writer) throws IOException {
        int newlineSuffixLen = findNewlineSuffixLen(line);
        if (newlineSuffixLen > 0) {
            writer.write(line, 0, line.length() - newlineSuffixLen);
            writer.write(System.lineSeparator());
        }
        else {
            writer.write(line);
            writer.write(System.lineSeparator());
            writer.write("\\ No newline at end of file");
            writer.write(System.lineSeparator());
        }
    }

    private static int findNewlineSuffixLen(String line) {
        if (line.endsWith("\r\n")) {
            return 2;
        }
        if (line.endsWith("\n") || line.endsWith("\r")) {
            return 1;
        }
        return 0;
    }
}