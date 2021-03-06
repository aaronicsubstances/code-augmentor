package com.aaronicsubstances.code.augmentor.core.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskLogLevel;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

/**
 * Exposes helper methods for generic tasks
 */
public class TaskUtils {
    public static final Pattern NEW_LINE_REGEX = Pattern.compile("\r\n|\r|\n");

    /**
     * Splits a string into lines. The list of lines can either be without line terminators, 
     * alternating in pairs with their line terminators.
     * If text doesn't end with a new line, then the last item is null.
     * <p>
     * Alternatively lines can have their terminators as suffixes, in which the size of 
     * return value will the same as the number of lines in text.
     * @param text string to split.
     * @param separateTerminators false to have lines include their terminators; true
     * to add line terminator as separate item in collection result
     * @return list of lines and terminators, with terminators included or excluded from lines
     * as determined by separateTerminators parameter.
     */
    public static List<String> splitIntoLines(String text, boolean separateTerminators) {
        List<String> splitText = new ArrayList<>();
        int lastEndIdx = 0;
        int[] temp = new int[2];
        while (true) {
            locateNewline(text, lastEndIdx, temp);
            int idx = temp[0];
            if (idx == -1) {
                break;
            }
            int newlineLen = temp[1];
            int endIdx = idx + newlineLen;
            if (separateTerminators) {
                String precedingLine = text.substring(lastEndIdx, idx);
                String terminatingNewline = text.substring(idx, endIdx);
                splitText.add(precedingLine);
                splitText.add(terminatingNewline);
            }
            else {
                String precedingLine = text.substring(lastEndIdx, endIdx);
                splitText.add(precedingLine);
            }
            lastEndIdx = endIdx;
        }
        if (lastEndIdx < text.length()) {
            splitText.add(text.substring(lastEndIdx));
            if (separateTerminators) {
                splitText.add(null);
            }
        }
        return splitText;
    }

    /**
     * Calculates line number given a position in a string.
     * 
     * @param content source code text.
     * @param position position in s.
     * 
     * @return line number.
     */
    public static int calculateLineNumber(String content, int position) {
        int lineNr = 1; // NB: line number starts from 1.
        int lastEndIdx = 0;
        int[] temp = new int[2];
        while (true) {
            locateNewline(content, lastEndIdx, temp);
            int idx = temp[0];
            if (idx == -1) {
                break;
            }
            int newlineLen = temp[1];
            int endIdx = idx + newlineLen;
            if (endIdx > position) {
                break;
            }
            lastEndIdx = endIdx;
            lineNr++;
        }
        return lineNr;
    }
    
    private static void locateNewline(String content, int start, int[] receipt) {
        boolean winLn = false;
        int idx = content.indexOf("\r\n", start);
        if (idx != -1) {
            winLn = true;
        }
        int idx2 = content.indexOf('\n', start);
        if (idx == -1) {
            idx = idx2;
            winLn = false;
        }
        else if (idx2 != -1 && idx2 < idx) {
            idx = idx2;
            winLn = false;
        }
        int idx3 = content.indexOf('\r', start);
        if (idx == -1) {
            idx = idx3;
            winLn = false;
        }
        else if (idx3 != -1 && idx3 < idx) {
            idx = idx3;
            winLn = false;
        }
        receipt[0] = idx;
        receipt[1] = winLn ? 2 : 1;
    }

    /**
     * Determines whether a character is a newline character.
     * @param ch character to test
     * @return true or false if ch is a newline character or not respectively.
     */
    public static boolean isNewLine(char ch) {
        return ch == '\r' || ch == '\n';
    }

    /**
     * Determines whether a string is null or has no characters.
     * @param s string to test
     * @return true only if s is null/empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Determines whether a string is null, has no characters, or has
     * only whitespace characters.
     * @param s string to test
     * @return true only if s is null/empty/whitespace only.
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Returns a string consisting of a concatenation of its duplicates 
     * by a given number of times. 
     * @param s string to duplicate
     * @param nTimes duplication count
     * @return multiplied string
     */
    public static String strMultiply(String s, int nTimes) {
        if (s == null) {
            return null;
        }
        if (nTimes == 0 || s.isEmpty()) {
            return "";
        }
        if (nTimes == 1) {
            return s;
        }
        StringBuilder multiplied = new StringBuilder();
        for (int i = 0; i < nTimes; i++) {
            multiplied.append(s);
        }
        return multiplied.toString();
    }

    /**
     * Determines whether a string is a valid JSON string.
     * @param s string to validate.
     * @return error message or null if s is valid JSON.
     */
    public static String validateJson(String s) {
        // cannot use JsonParser.parse*() methods since they always
        // are used by GSON (as at 2.8.6) in lenient mode. 
        try (JsonReader reader = new JsonReader(new StringReader(s))) {
            reader.setLenient(false);
            reader.skipValue();
            JsonToken next = reader.peek();
            assert next.equals(JsonToken.END_DOCUMENT);
            return null;
        }
        catch (Exception ex) {            
            String message = ex.getMessage();
            if (ex instanceof MalformedJsonException) {
                String cutOutPrefix = "Use JsonReader.setLenient(true) to accept malformed JSON";
                if (message.startsWith(cutOutPrefix)) {
                    message = message.substring(cutOutPrefix.length()).trim();
                }
                return MalformedJsonException.class.getSimpleName() + ": " + message;
            }
            else if (ex instanceof EOFException) {
                String cutOutPrefix = "End of input";
                if (message.startsWith(cutOutPrefix)) {
                    message = "Unexpected end of input " + 
                        message.substring(cutOutPrefix.length()).trim();
                }
                if (isBlank(s)) {
                    message += " (input is blank)";
                }
                return EOFException.class.getSimpleName() + ": " + message;
            }
            return ex.toString();
        }
    }

    /**
     * Generates a name with a given prefix which is guaranteed to be absent in a given list. If 
     * prefix is not in the list in the first place, then prefix is simply returned.
     * @param names given list.
     * @param originalName given prefix.
     * @return a name which has originalName as a prefix and is not in names list.
     */
    public static String modifyNameToBeAbsent(Collection<String> names, String originalName) {
        if (!names.contains(originalName)) {
            return originalName;
        }
        StringBuilder modifiedName = new StringBuilder(originalName);
        int index = 1;
        modifiedName.append("-").append(index);
        while (names.contains(modifiedName.toString())) {
            index++;
            modifiedName.setLength(originalName.length());
            modifiedName.append("-").append(index);
        }
        return modifiedName.toString();
    }

    /**
     * Calculate MD5 hash of file 
     * @param contents file contents
     * @param charset file encoding
     * @return MD5 hash of file as hexadecimal (lowercase) string.
     * @throws Exception
     */
    public static String calcHash(String contents, Charset charset) throws Exception {
        byte[] binaryContent = contents.getBytes(charset);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(binaryContent);
        binaryContent = md.digest();
        BigInteger bigInteger = new BigInteger(1, binaryContent);
        int outputLen = binaryContent.length * 2;
        return String.format("%0" + outputLen + "x", bigInteger);
    }

    public static String readFile(File srcFile, Charset charset) throws IOException {
        byte[] buf = Files.readAllBytes(srcFile.toPath());
        String s = new String(buf, charset);
        return s;
    }

    public static void writeFile(File destFile, Charset charset, String contents) throws IOException {
        Files.write(destFile.toPath(), contents.getBytes(charset));
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes a directory recursively. Silently ignores any deletion failures.
     * @param dir directory to delete.
     */
	public static void deleteDir(File dir) {
        // ignore unsuccessful deletion and any errors.
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File content : contents) {
                if (content.isDirectory()) {
                    deleteDir(content);
                }
                else {
                    content.delete();
                }
            }
        }
        dir.delete();
	}

    public static void logVerbose(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender, 
            String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(GenericTaskLogLevel.VERBOSE, () -> String.format(format, args));
    }

    public static void logInfo(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender, 
            String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(GenericTaskLogLevel.INFO, () -> String.format(format, args));        
    }

    public static void logWarn(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender, 
            String format, Object... args) {
        if (logAppender == null) {
            return;
        }
        logAppender.accept(GenericTaskLogLevel.WARN, () -> String.format(format, args));        
    }
}