package com.aaronicsubstances.code.augmentor.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;
import com.google.gson.annotations.SerializedName;

public class CodeGenerationRequest {
    @SerializedName("files")
    private List<SourceFileAugmentingCode> sourceFileAugmentingCodeList;

    public CodeGenerationRequest() {
    }

    public CodeGenerationRequest(List<SourceFileAugmentingCode> sourceFileAugmentingCodeList) {
        this.sourceFileAugmentingCodeList = sourceFileAugmentingCodeList;
    }

    public List<SourceFileAugmentingCode> getSourceFileAugmentingCodeList() {
        return sourceFileAugmentingCodeList;
    }

    public void setSourceFileAugmentingCodeList(List<SourceFileAugmentingCode> sourceFileAugmentingCodeList) {
        this.sourceFileAugmentingCodeList = sourceFileAugmentingCodeList;
    }

    public Object beginSerialize(File file) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file),
            StandardCharsets.UTF_8);
        return beginSerialize(writer);
    }

    public Object beginSerialize(Writer writer) {
        PrintWriter pW = new PrintWriter(writer, true);
        return pW;
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer == null) {
            return;
        }
        Writer writer = (Writer) serializer;
        writer.close();
    }

	public void serialize(File file, boolean serializeAllAsJson) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer, serializeAllAsJson);
        }
	}

	public void serialize(Writer writer, boolean serializeAllAsJson) throws Exception {
        if (serializeAllAsJson) {
            String json = TaskUtils.serializeToJson(sourceFileAugmentingCodeList);
            writer.write(json);
            writer.flush();
        }
        else {
            PrintWriter pW = new PrintWriter(writer);
            for (SourceFileAugmentingCode s : sourceFileAugmentingCodeList) {
                s.serialize(pW);
            }
            pW.flush();
        }
	}

    public Object beginDeserialize(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8);
        return beginDeserialize(reader);
    }

    public Object beginDeserialize(Reader reader) throws Exception {
        BufferedReader bufRdr = new BufferedReader(reader);
        boolean perFile = TaskUtils.peekSerializedJsonForPerFile(bufRdr);
        if (perFile) {
            String json = TaskUtils.readFully(bufRdr);
            SourceFileAugmentingCode[] files = TaskUtils.deserializeFromJson(json,
                SourceFileAugmentingCode[].class);
            sourceFileAugmentingCodeList = Arrays.asList(files);
        }
        else {
            sourceFileAugmentingCodeList = new ArrayList<>();
        }

        return bufRdr;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        if (deserializer == null) {
            return;
        }
        Reader rdr = (Reader) deserializer;
        rdr.close();
    }

    public static CodeGenerationRequest deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

    public static CodeGenerationRequest deserialize(Reader reader) throws Exception {
        CodeGenerationRequest instance = new CodeGenerationRequest();
        BufferedReader bufRdr = (BufferedReader) instance.beginDeserialize(reader);
        SourceFileAugmentingCode s;
        while ((s = SourceFileAugmentingCode.deserialize(bufRdr)) != null) {
            instance.sourceFileAugmentingCodeList.add(s);
        }
        return instance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceFileAugmentingCodeList == null) ? 0 : sourceFileAugmentingCodeList.hashCode());
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
        CodeGenerationRequest other = (CodeGenerationRequest) obj;
        if (sourceFileAugmentingCodeList == null) {
            if (other.sourceFileAugmentingCodeList != null)
                return false;
        } else if (!sourceFileAugmentingCodeList.equals(other.sourceFileAugmentingCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{sourceFileAugmentingCodeList=" + sourceFileAugmentingCodeList + "}";
    }
}