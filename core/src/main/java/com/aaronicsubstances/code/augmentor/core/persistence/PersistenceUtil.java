package com.aaronicsubstances.code.augmentor.core.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class PersistenceUtil {
    public static final Gson JSON_CONVERT = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson JSON_CONVERT_COMPACT = new Gson();

	public static String serializeCompactlyToJson(Object obj) {
		return JSON_CONVERT_COMPACT.toJson(obj);
	}

	public static String serializeToJson(Object obj) {
		return JSON_CONVERT.toJson(obj);
	}

	public static <T> T deserializeFromJson(String s, Class<T> cls) {
		return JSON_CONVERT.fromJson(s, cls);
	}

	public static <T> T deserializeFromJson(Reader rdr, Class<T> cls) {
		return JSON_CONVERT.fromJson(rdr, cls);
	}

	public static boolean peekSerializedJsonForPerFile(BufferedReader bufRdr) throws IOException {		
        // decide between per-line json or per-file json
        boolean perFile = false;
        bufRdr.mark(1);
        int firstCh = bufRdr.read();
        bufRdr.reset();
        if (firstCh == '[') {
            perFile = true;
        }
        return perFile;
	}

    private final BufferedReader bufferedReader;
    private final JsonReader jsonReader;
    private final PrintWriter printWriter;
    private final JsonWriter jsonWriter;
    private final boolean closeWhenDone;

    public PersistenceUtil(BufferedReader reader, boolean closeWhenDone) {
        this.bufferedReader = reader;
        this.jsonReader = null;
        this.printWriter = null;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(JsonReader reader, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.jsonReader = reader;
        this.printWriter = null;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(PrintWriter writer, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.jsonReader = null;
        this.printWriter = writer;
        this.jsonWriter = null;
        this.closeWhenDone = closeWhenDone;
    }

    public PersistenceUtil(JsonWriter writer, boolean closeWhenDone) {
        this.bufferedReader = null;
        this.jsonReader = null;
        this.printWriter = null;
        this.jsonWriter = writer;
        this.closeWhenDone = closeWhenDone;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public JsonReader getJsonReader() {
        return jsonReader;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public JsonWriter getJsonWriter() {
        return jsonWriter;
    }

    public boolean isCloseWhenDone() {
        return closeWhenDone;
    }
}