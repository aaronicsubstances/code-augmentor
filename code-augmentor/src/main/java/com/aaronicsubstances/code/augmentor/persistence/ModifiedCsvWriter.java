package com.aaronicsubstances.code.augmentor.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */
public class ModifiedCsvWriter implements AutoCloseable {
    static final String COMMENT_PREFIX = "#";
    static final String FIELDS_SPEC_PREFIX = "#fields=";
    private static final Pattern ESCAPES;

    static {
        char[] escapeRequiringChars = {'&', ',', '"', '\r', '\n', '#'};
        StringBuilder regex = new StringBuilder();
        for (char ch : escapeRequiringChars) {
            if (regex.length() > 0) {
                regex.append("|");
            }
            regex.append(Pattern.quote(String.valueOf(ch)));
        }
        ESCAPES = Pattern.compile(regex.toString());
    }

    private final Writer writer;
    private String[] fields;

    public ModifiedCsvWriter(File file) throws IOException {
        writer = new OutputStreamWriter(new FileOutputStream(file),
            StandardCharsets.UTF_8);
    }

    public ModifiedCsvWriter(Writer writer) {
        this.writer = writer;
    }
    
    @Override
    public void close() throws Exception {
        writer.close();
    }

    public void writeFields(String[] fields) throws IOException {
        if (this.fields != null) {
            throw new RuntimeException("fields have already been written");
        }
        Set<String> orderedFieldSet = new LinkedHashSet<>(Arrays.asList(fields));
        if (fields.length != orderedFieldSet.size()) {
            throw new RuntimeException("fields contain duplicates");
        }

        // Copy fields over 
        this.fields = new String[fields.length];
        orderedFieldSet.toArray(this.fields);

        // build field into record string
        String recordString = buildRecordString(this.fields);
        writer.write(FIELDS_SPEC_PREFIX + recordString + System.lineSeparator());
    }

    public void writeRecord(Map<String, String> recordMap) throws IOException {
        if (fields == null) {
            throw new RuntimeException("fields are yet to be written");
        }
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (recordMap.containsKey(field)) {
                values[i] = recordMap.get(field);
            }
            if (values[i] == null) {
                values[i] = "";
            }
        }
        
        String recordString = buildRecordString(values);
        writer.write(recordString + System.lineSeparator());
    }

    public void writeRecord(Object[] record) throws IOException {
        if (fields != null && record.length != fields.length) {
            throw new RuntimeException(String.format("Expected %s record entries, " +
                "but received %s", fields.length, record.length));
        }
        String[] values = new String[record.length];
        for (int i = 0; i < values.length; i++) {
            Object recordEntry = record[i];
            if (recordEntry != null) {
                values[i] = recordEntry.toString();
            }
            if (values[i] == null) {
                values[i] = "";
            }
        }
        
        String recordString = buildRecordString(values);
        writer.write(recordString + System.lineSeparator());
    }

    private String buildRecordString(String[] values) {
        StringBuilder recordString = new StringBuilder();
        for (String value : values) {
            if (recordString.length() > 0) {
                recordString.append(',');
            }
            // escape value before adding.
            Matcher m = ESCAPES.matcher(value);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                int chValue = m.group().charAt(0);
                String replacement = Matcher.quoteReplacement("&#" + chValue + ";");
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            recordString.append(sb);
        }
        return recordString.toString();
    }

    public void writeComment(String comment) throws IOException  {
        if (comment.contains("\r") || comment.contains("\n")) {
            throw new RuntimeException("new line characters found in comment");
        }
        writer.write(COMMENT_PREFIX + comment + System.lineSeparator());
    }

    public void writeSeparatorLine() throws IOException {
        writer.write(System.lineSeparator());
    }
}