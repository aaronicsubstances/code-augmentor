package com.aaronicsubstances.code.augmentor.core.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class CodeGenerationResponseChangeSet {
    private List<SourceFileChangeSet> sourceFileChangeSets;

    public CodeGenerationResponseChangeSet() {        
    }

    public CodeGenerationResponseChangeSet(List<SourceFileChangeSet> sourceFileChangeSets) {
        this.sourceFileChangeSets = sourceFileChangeSets;
    }

    public List<SourceFileChangeSet> getSourceFileChangeSets() {
        return sourceFileChangeSets;
    }

    public void setSourceFileChangeSets(List<SourceFileChangeSet> sourceFileChangeSets) {
        this.sourceFileChangeSets = sourceFileChangeSets;
    }

    public Object beginSerialize(File file) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file),
            StandardCharsets.UTF_8);
        return beginSerialize(writer, true);
    }

    public Object beginSerialize(Writer writer) throws Exception {
        return beginSerialize(writer, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) throws Exception {
        JsonWriter writer = new JsonWriter(stream);
        writer.setIndent("  ");
        writer.beginObject();
        writer.name("files");
        writer.beginArray();
        PersistenceUtil persistenceUtil= new PersistenceUtil(writer, closeStream);
        return persistenceUtil;
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = ((PersistenceUtil) serializer);
        JsonWriter writer = persistenceUtil.getJsonWriter();
        writer.endArray();
        writer.endObject();
        try {
            persistenceUtil.flush();
        }
        finally {
            persistenceUtil.close();
        }
    }

    public void serialize(File file) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer);
        }
    }

    public void serialize(Writer stream) throws Exception {
        Object serializer = beginSerialize(stream, false);
        for (SourceFileChangeSet s : sourceFileChangeSets) {
            s.serialize(serializer);
        }
        endSerialize(serializer);
    }

    public Object beginDeserialize(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8);
        return beginDeserialize(reader, true);
    }

    public Object beginDeserialize(Reader reader) throws Exception {
        return beginDeserialize(reader, false);
    }

    private PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        JsonReader jsonReader = new JsonReader(stream);
        jsonReader.beginObject();
        // skip until we get to files.
        while (jsonReader.hasNext()) {
            JsonToken token = jsonReader.peek();
            if (token == JsonToken.NAME && jsonReader.nextName().equals("files")) {
                break;
            }
            jsonReader.skipValue();
        }
        jsonReader.beginArray();
        PersistenceUtil persistenceUtil = new PersistenceUtil(jsonReader, 
            closeStream);
        sourceFileChangeSets = new ArrayList<>(); 
        return persistenceUtil;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        persistenceUtil.close();
    }

    public static CodeGenerationResponseChangeSet deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

    public static CodeGenerationResponseChangeSet deserialize(Reader reader) throws Exception {
        CodeGenerationResponseChangeSet instance = new CodeGenerationResponseChangeSet();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            SourceFileChangeSet s;
            while ((s = SourceFileChangeSet.deserialize(deserializer)) != null) {
                instance.sourceFileChangeSets.add(s);
            }
        }
        finally {
            instance.endDeserialize(deserializer);
        }
        return instance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceFileChangeSets == null) ? 0 : sourceFileChangeSets.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CodeGenerationResponseChangeSet other = (CodeGenerationResponseChangeSet) obj;
        if (sourceFileChangeSets == null) {
            if (other.sourceFileChangeSets != null)
                return false;
        } else if (!sourceFileChangeSets.equals(other.sourceFileChangeSets))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationResponseChangeSet{sourceFileChangeSets=" + sourceFileChangeSets + "}";
    }
}