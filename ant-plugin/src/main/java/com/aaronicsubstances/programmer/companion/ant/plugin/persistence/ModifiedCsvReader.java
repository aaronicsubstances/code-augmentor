package com.aaronicsubstances.programmer.companion.ant.plugin.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */
public class ModifiedCsvReader implements AutoCloseable {
    public static final int LINE_TYPE_FIELD = 0;
    public static final int LINE_TYPE_SEPARATOR = 1;
    public static final int LINE_TYPE_COMMENT = 2;
    public static final int LINE_TYPE_RECORD = 3;

    private static final Pattern ESCAPES;

    static  {
        StringBuilder regex = new StringBuilder();
        regex.append(Pattern.quote("&#"));
        regex.append("([+-]*\\d+)");
        regex.append(Pattern.quote(";"));
        ESCAPES = Pattern.compile(regex.toString());
    }
    
    private final BufferedReader reader;
    private String[] fields;
    private int lineNumber;

    public ModifiedCsvReader(File file) throws IOException {
        reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8));
    }

    ModifiedCsvReader(BufferedReader reader) {
        this.reader = reader;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    public int read(Object[] receiver) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.startsWith(ModifiedCsvWriter.COMMENT_PREFIX)) {
                if (line.startsWith(ModifiedCsvWriter.FIELDS_SPEC_PREFIX)) {
                    if (fields != null) {
                        throw createAbortException("fields have already been read");
                    }
                    String recordString = line.substring(
                        ModifiedCsvWriter.FIELDS_SPEC_PREFIX.length());
                    fields = retrieveRecord(recordString);
                    receiver[0] = fields;
                    return LINE_TYPE_FIELD;
                }
                else {
                    receiver[0] = line.substring(1);
                    return LINE_TYPE_COMMENT;
                }
            }
            else if (line.trim().isEmpty()) {
                receiver[0] = "";
                return LINE_TYPE_SEPARATOR;
            }
            else {
                String[] record = retrieveRecord(line);
                if (fields != null && record.length != fields.length) {
                    throw createAbortException(String.format("Expected %s record entries, " +
                        "but received %s", fields.length, record.length));
                }
                receiver[0] = record;
                return LINE_TYPE_RECORD;
            }
        }
        receiver[0] = null;
        return -1;
    }

    private String[] retrieveRecord(String recordString) {
        // record is comma separated, 
        String[] record = recordString.split(",", -1);
        // finally, unescape entries.
        for (int i = 0; i < record.length; i++) {
            StringBuffer sb = new StringBuffer();
            Matcher m = ESCAPES.matcher(record[i]);
            while (m.find()) {
                String codePointString = m.group(1);
                int codePoint;
                try {
                    codePoint = Integer.parseInt(codePointString);
                    if (codePoint < 0) {
                        throw createAbortException("Invalid escape sequence: " + m.group());
                    }
                    if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        throw createAbortException("Invalid escape sequence " +
                            "(only Unicode BMP supported): " + m.group());
                    }
                }
                catch (NumberFormatException ex) {
                    throw createAbortException("Invalid escape sequence: " + m.group());
                }
                String replacement = String.valueOf((char)codePoint);
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            record[i] = sb.toString();
        }
        return record;
    }

    public Map<String, String> convertRecordToDict(String[] record) {
        if (fields == null) {
            throw createAbortException("fields are yet to be read");
        }
        if (record.length != fields.length) {
            throw createAbortException(String.format("Expected %s record entries, " +
                "but received %s", fields.length, record.length));
        }
        Map<String, String> dict = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            String value = record[i];
            dict.put(field, value);
        }
        return dict;
    }

    private RuntimeException createAbortException(String msg) {
        return new RuntimeException(String.format("Ln %s: %s", lineNumber, msg));
    }
}