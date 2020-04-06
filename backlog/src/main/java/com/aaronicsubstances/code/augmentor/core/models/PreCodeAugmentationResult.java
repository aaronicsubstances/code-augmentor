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

import com.aaronicsubstances.code.augmentor.core.persistence.PersistenceUtil;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class PreCodeAugmentationResult {
    @SerializedName("gen_code_start_suffix")
    private String genCodeStartSuffix;
    @SerializedName("gen_code_end_suffix")
    private String genCodeEndSuffix;

    // always ensure this list comes last when adding new fields.
    @SerializedName("files")
    private List<SourceFileDescriptor> fileDescriptors;

    public PreCodeAugmentationResult() {
    }

    public PreCodeAugmentationResult(List<SourceFileDescriptor> fileDescriptors) {
        this.fileDescriptors = fileDescriptors;
    }

    public List<SourceFileDescriptor> getFileDescriptors() {
        return fileDescriptors;
    }

    public void setFileDescriptors(List<SourceFileDescriptor> fileDescriptors) {
        this.fileDescriptors = fileDescriptors;
    }

    public String getGenCodeStartSuffix() {
        return genCodeStartSuffix;
    }

    public void setGenCodeStartSuffix(String genCodeStartSuffix) {
        this.genCodeStartSuffix = genCodeStartSuffix;
    }

    public String getGenCodeEndSuffix() {
        return genCodeEndSuffix;
    }

    public void setGenCodeEndSuffix(String genCodeEndSuffix) {
        this.genCodeEndSuffix = genCodeEndSuffix;
    }

    public Object beginSerialize(File file) throws Exception {        
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream, true);
        return serializer;
    }
    public Object beginSerialize(Writer stream) throws Exception {
        return beginSerialize(stream, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) throws Exception {
        JsonWriter writer = new JsonWriter(stream);
        writer.setIndent("  ");
        writer.beginObject();
        writer.name("gen_code_start_suffix").value(genCodeStartSuffix);
        writer.name("gen_code_end_suffix").value(genCodeEndSuffix);
        writer.name("files");
        writer.beginArray();
        return new PersistenceUtil(writer, closeStream);
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        JsonWriter writer = persistenceUtil.getJsonWriter();
        try {
            writer.endArray();
            writer.endObject();
            writer.flush();
        }
        finally {
            if (persistenceUtil.isCloseWhenDone()) {
                writer.close();
            }
        }
    }

    public void serialize(File file) throws Exception {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer);
        }
    }

    public void serialize(Writer writer) throws Exception {
        String json = PersistenceUtil.serializeToJson(this);
        writer.write(json);
        writer.flush();
    }

    public Object beginDeserialize(File file) throws Exception {    
        InputStreamReader stream = new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginDeserialize(stream, true);
        return serializer;
    }

    public Object beginDeserialize(Reader stream) throws Exception {
        return beginDeserialize(stream, false);
    }

    private PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        JsonReader reader = new JsonReader(stream);
        reader.beginObject();
        String name;
        while (!(name = reader.nextName()).equals("files")) {            ;
            switch (name) {
                case "gen_code_start_suffix":
                    genCodeStartSuffix = reader.nextString();
                    break;
                case "gen_code_end_suffix":
                    genCodeEndSuffix = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.beginArray();

        fileDescriptors = new ArrayList<>();
        return new PersistenceUtil(reader, closeStream);
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        JsonReader reader = persistenceUtil.getJsonReader();
        if (persistenceUtil.isCloseWhenDone()) {
            reader.close();
        }
    }

    public static PreCodeAugmentationResult deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), 
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

	public static PreCodeAugmentationResult deserialize(Reader reader) throws Exception {
        PreCodeAugmentationResult instance = new PreCodeAugmentationResult();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            SourceFileDescriptor s;
            while ((s = SourceFileDescriptor.deserialize(deserializer)) != null) {
                instance.getFileDescriptors().add(s);
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
        result = prime * result + ((fileDescriptors == null) ? 0 : fileDescriptors.hashCode());
        result = prime * result + ((genCodeEndSuffix == null) ? 0 : genCodeEndSuffix.hashCode());
        result = prime * result + ((genCodeStartSuffix == null) ? 0 : genCodeStartSuffix.hashCode());
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
        PreCodeAugmentationResult other = (PreCodeAugmentationResult) obj;
        if (fileDescriptors == null) {
            if (other.fileDescriptors != null)
                return false;
        } else if (!fileDescriptors.equals(other.fileDescriptors))
            return false;
        if (genCodeEndSuffix == null) {
            if (other.genCodeEndSuffix != null)
                return false;
        } else if (!genCodeEndSuffix.equals(other.genCodeEndSuffix))
            return false;
        if (genCodeStartSuffix == null) {
            if (other.genCodeStartSuffix != null)
                return false;
        } else if (!genCodeStartSuffix.equals(other.genCodeStartSuffix))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PreCodeAugmentationResult{fileDescriptors=" + fileDescriptors + ", genCodeEndSuffix="
                + genCodeEndSuffix + ", genCodeStartSuffix=" + genCodeStartSuffix + "}";
    }
}