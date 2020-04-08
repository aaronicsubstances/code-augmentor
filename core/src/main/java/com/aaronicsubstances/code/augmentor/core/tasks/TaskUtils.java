package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Exposes helper methods for generic tasks
 */
public class TaskUtils {

    /**
     * Splits a string into lines.
     * @param text string to split.
     * @return list of lines without line terminators, alternating in pairs with their line terminators.
     * If text doesn't end with a new line, then the last item is null.
     */
    public static List<String> splitIntoLines(String text) {
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
            String precedingLine = text.substring(lastEndIdx, idx);
            String terminatingNewline = text.substring(idx, endIdx);
            splitText.add(precedingLine);
            splitText.add(terminatingNewline);
            lastEndIdx = endIdx;
        }
        if (lastEndIdx < text.length()) {
            splitText.add(text.substring(lastEndIdx));
            splitText.add(null);
        }
        return splitText;
    }

    /**
     * Calculates line number given a position in a string.
     * 
     * @param s source code text.
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

    public static boolean isNewLine(char ch) {
        return ch == '\r' || ch == '\n';
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isValidJsonArray(String s) {
        try {
            JsonElement json = JsonParser.parseString(s);
            return json instanceof JsonArray;
        }
        catch (JsonParseException ex) {
            return false;
        }
    }

    public static String modifyNameToBeAbsent(Collection<String> names, String originalName) {
        if (!names.contains(originalName)) {
            return originalName;
        }
        StringBuilder modifiedName = new StringBuilder(originalName);
        int index = 1;
        modifiedName.append("-").append(index);
        while (names.contains(originalName)) {   
            index++;
            modifiedName.setLength(originalName.length());
            modifiedName.append("-").append(index);
        }
        return modifiedName.toString();
    }

    public static String readFile(File srcFile, Charset charset) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try (InputStream inStream = new FileInputStream(srcFile)) {
            copyStream(inStream, outStream);
            outStream.flush();
        }
        byte[] buf = outStream.toByteArray();
        String s = new String(buf, charset);
        return s;
    }

    public static void writeFile(File destFile, Charset charset, String contents) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(destFile), charset)) {
            writer.write(contents);
        }
    }

    public static String calcHash(String contents, Charset charset) throws NoSuchAlgorithmException {
        byte[] binaryContent = contents.getBytes(charset);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(binaryContent);
        binaryContent = md.digest();
        String hash = Base64.getEncoder().encodeToString(binaryContent);
        return hash;
    }

    public static void copyStream(InputStream inStream, OutputStream outStream) throws IOException {
        byte[] b  = new byte[8192];
        int len;
        while ((len = inStream.read(b)) > 0) {
            outStream.write(b, 0, len);
        }
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(srcFile)) {
            try (OutputStream outputStream = new FileOutputStream(destFile)) {
                copyStream(inputStream, outputStream);
            }
        }
    }
}