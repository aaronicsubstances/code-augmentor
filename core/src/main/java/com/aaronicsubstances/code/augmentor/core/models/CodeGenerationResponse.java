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

public class CodeGenerationResponse {

    public static class Header {
        @SerializedName("newline")
        String newline;
        @SerializedName("content_streaming_enabled")
        Boolean contentStreamEnabled;
    }
    
    private String newline;
    private List<SourceFileGeneratedCode> sourceFileGeneratedCodeList;

    public CodeGenerationResponse() {
    }

    public CodeGenerationResponse(List<SourceFileGeneratedCode> sourceFileGeneratedCodeList) {
        this.sourceFileGeneratedCodeList = sourceFileGeneratedCodeList;
    }

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public List<SourceFileGeneratedCode> getSourceFileGeneratedCodeList() {
        return sourceFileGeneratedCodeList;
    }

    public void setSourceFileGeneratedCodeList(List<SourceFileGeneratedCode> sourceFileGeneratedCodeList) {
        this.sourceFileGeneratedCodeList = sourceFileGeneratedCodeList;
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
        PersistenceUtil persistenceUtil= new PersistenceUtil(new PrintWriter(stream), closeStream);
        printHeader(persistenceUtil, true);
        return persistenceUtil;
    }

    private void printHeader(PersistenceUtil persistenceUtil, boolean contentStreamEnabled) 
            throws Exception {
        Header header = new Header();
        header.newline = newline;
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
            String json = PersistenceUtil.serializeFormattedToJson(sourceFileGeneratedCodeList);
            persistenceUtil.println(json);
        }
        else {
            for (SourceFileGeneratedCode s : sourceFileGeneratedCodeList) {
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

    private PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new BufferedReader(stream), 
            closeStream);
        boolean contentStreamEnabled = readHeader(persistenceUtil);
        sourceFileGeneratedCodeList = new ArrayList<>();
        if (!contentStreamEnabled) {            
            String inputRemainder = persistenceUtil.readToEnd();
            SourceFileGeneratedCode[] entireList = PersistenceUtil.deserializeFromJson(inputRemainder, 
                SourceFileGeneratedCode[].class);
            persistenceUtil.setContent(entireList);
        }
        return persistenceUtil;
    }

    private boolean readHeader(PersistenceUtil persistenceUtil) throws Exception {
        String headerString = persistenceUtil.readLine();
        Header header = PersistenceUtil.deserializeFromJson(headerString, Header.class);
        newline = header.newline;
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

    public static CodeGenerationResponse deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

    public static CodeGenerationResponse deserialize(Reader reader) throws Exception {
        CodeGenerationResponse instance = new CodeGenerationResponse();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            SourceFileGeneratedCode s;
            while ((s = SourceFileGeneratedCode.deserialize(deserializer)) != null) {
                instance.sourceFileGeneratedCodeList.add(s);
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
        result = prime * result + ((newline == null) ? 0 : newline.hashCode());
        result = prime * result + ((sourceFileGeneratedCodeList == null) ? 0 : sourceFileGeneratedCodeList.hashCode());
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
        CodeGenerationResponse other = (CodeGenerationResponse) obj;
        if (newline == null) {
            if (other.newline != null)
                return false;
        } else if (!newline.equals(other.newline))
            return false;
        if (sourceFileGeneratedCodeList == null) {
            if (other.sourceFileGeneratedCodeList != null)
                return false;
        } else if (!sourceFileGeneratedCodeList.equals(other.sourceFileGeneratedCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationResponse{newline=" + newline + ", sourceFileGeneratedCodeList="
                + sourceFileGeneratedCodeList + "}";
    }
}