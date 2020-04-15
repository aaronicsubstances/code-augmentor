package com.aaronicsubstances.code.augmentor.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PersistenceUtil {
    private static final Gson JSON_CONVERT = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson JSON_CONVERT_COMPACT = new Gson();

    public static String serializeCompactlyToJson(Object obj) {
        return JSON_CONVERT_COMPACT.toJson(obj);
    }

    public static String serializeFormattedToJson(Object obj) {
        return JSON_CONVERT.toJson(obj);
    }

    public static <T> T deserializeFromJson(String s, Class<T> cls) {
        if (TaskUtils.isBlank(s)) {
            throw new RuntimeException("Cannot parse blank string as JSON");
        }
        return JSON_CONVERT.fromJson(s, cls);
    }

    private final BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private final boolean closeWhenDone;
    
    private Object content;
    private int contentIndex;

    public PersistenceUtil(BufferedReader reader, boolean closeWhenDone) {
        this.bufferedReader = reader;
        this.printWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(PrintWriter writer, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.printWriter = writer;
        this.closeWhenDone = closeWhenDone;
    }

    public void println(String s) throws IOException {
        printWriter.println(s);
    }

    public void flush() throws Exception {
        printWriter.flush();
    }

    public String readToEnd() throws IOException {
        StringBuilder s = new StringBuilder();
        char[] buf = new char[8192];
        int len;
        while ((len = bufferedReader.read(buf)) > 0) {
            s.append(buf, 0, len);
        }
        return s.toString();
    }

    public String readLine() throws IOException {
        return bufferedReader.readLine();
    }

    public void close() throws IOException {
        if (!closeWhenDone) {
            return;
        }
        if (bufferedReader != null) {
            bufferedReader.close();
        }
        if (printWriter != null) {
            printWriter.close();
        }
    }

    public boolean isCloseWhenDone() {
        return closeWhenDone;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public int getContentIndex() {
        return contentIndex;
    }

    public void setContentIndex(int contentIndex) {
        this.contentIndex = contentIndex;
    }
}