package com.aaronicsubstances.code.augmentor.core.models;

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
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.persistence.PersistenceUtil;
import com.google.gson.annotations.SerializedName;

public class CodeGenerationRequest {

    public static class Header {
        @SerializedName("content_streaming_enabled")
        Boolean contentStreamEnabled;
    }

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
        return beginSerialize(writer, true);
    }

    public Object beginSerialize(Writer stream) throws Exception {
        return beginSerialize(stream, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) throws Exception {
        PersistenceUtil persistenceUtil= new PersistenceUtil(new PrintWriter(stream), closeStream);
        printHeader(persistenceUtil, true);
        return persistenceUtil;
    }

    private void printHeader(PersistenceUtil persistenceUtil, boolean contentStreamEnabled) 
            throws Exception {        
        Header header = new Header();
        header.contentStreamEnabled = contentStreamEnabled;
        String headerString = PersistenceUtil.serializeCompactlyToJson(header);
        persistenceUtil.println(headerString);
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        try {
            persistenceUtil.flush();
        }
        finally {
            persistenceUtil.close();
        }
    }

    public void serialize(File file, boolean serializeAllAsJson) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer, serializeAllAsJson);
        }
    }

    public void serialize(Writer stream, boolean serializeAllAsJson) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new PrintWriter(stream), false);
        printHeader(persistenceUtil, !serializeAllAsJson);
        if (serializeAllAsJson) {
            String json = PersistenceUtil.serializeFormattedToJson(sourceFileAugmentingCodeList);
            persistenceUtil.println(json);
        }
        else {
            for (SourceFileAugmentingCode s : sourceFileAugmentingCodeList) {
                s.serialize(persistenceUtil);
            }
        }
        persistenceUtil.flush();
    }

    public Object beginDeserialize(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8);
        return beginDeserialize(reader, true);
    }

    public Object beginDeserialize(Reader reader) throws Exception {
        return beginDeserialize(reader, false);
    }

    public PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new BufferedReader(stream), 
            closeStream);
        boolean contentStreamEnabled = readHeader(persistenceUtil);
        sourceFileAugmentingCodeList = new ArrayList<>();
        if (!contentStreamEnabled) {            
            String inputRemainder = persistenceUtil.readToEnd();
            SourceFileAugmentingCode[] entireList = PersistenceUtil.deserializeFromJson(inputRemainder, 
                SourceFileAugmentingCode[].class);
            persistenceUtil.setContent(entireList);
        }
        return persistenceUtil;
    }

    private boolean readHeader(PersistenceUtil persistenceUtil) throws Exception {
        String headerString = persistenceUtil.readLine();
        Header header = PersistenceUtil.deserializeFromJson(headerString, Header.class);
        // enable content streaming by default.
        boolean contentStreamEnabled = true;
        if (header.contentStreamEnabled != null) {
            contentStreamEnabled = header.contentStreamEnabled;
        }
        return contentStreamEnabled;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        persistenceUtil.close();
    }

    public static CodeGenerationRequest deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

    public static CodeGenerationRequest deserialize(Reader reader) throws Exception {
        CodeGenerationRequest instance = new CodeGenerationRequest();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            SourceFileAugmentingCode s;
            while ((s = SourceFileAugmentingCode.deserialize(deserializer)) != null) {
                instance.sourceFileAugmentingCodeList.add(s);
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