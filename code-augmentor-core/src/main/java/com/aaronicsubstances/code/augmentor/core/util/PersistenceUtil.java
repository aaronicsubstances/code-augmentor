package com.aaronicsubstances.code.augmentor.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Used to encapsulate streaming read/write operations of files with models used by
 * Code Augmentor.
 */
public class PersistenceUtil {
    private static final Gson JSON_CONVERT = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson JSON_CONVERT_COMPACT = new Gson();

    /***
     * Serializes object so that no newline is present.
     * @param obj object to serialize
     * @return serialized object
     */
    public static String serializeCompactlyToJson(Object obj) {
        return JSON_CONVERT_COMPACT.toJson(obj);
    }

    /***
     * Serializes object with whitespace formatting so that during testing 
     * output files can be conveniently investigated.
     * @param obj object to serialize
     * @return serialized object
     */
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
    private final BufferedWriter bufferedWriter;
    private JsonReader jsonReader;
    private JsonWriter jsonWriter;
    private final boolean closeWhenDone;
    
    private Object content;
    private int contentIndex;

    public PersistenceUtil(BufferedReader reader, boolean closeWhenDone) {
        this.bufferedReader = reader;
        this.bufferedWriter = null;
        this.jsonReader = null;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(BufferedWriter writer, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.bufferedWriter = writer;
        this.jsonReader = null;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(JsonReader reader, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.bufferedWriter = null;
        this.jsonReader = reader;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(JsonWriter writer, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.bufferedWriter = null;
        this.jsonReader = null;
        this.jsonWriter = writer;
        this.closeWhenDone = closeWhenDone;
    }

    public void println(String s) throws IOException {
        bufferedWriter.write(s);
        bufferedWriter.newLine();
    }

    public void flush() throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush();
        }
        if (jsonWriter != null) {
            jsonWriter.flush();
        }
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
        if (bufferedWriter != null) {
            bufferedWriter.close();
        }
        if (jsonReader != null) {
            jsonReader.close();
        }
        if (jsonWriter != null) {
            jsonWriter.close();
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

	public JsonReader getJsonReader() {
		return jsonReader;
	}

	public JsonWriter getJsonWriter() {
		return jsonWriter;
	}
}