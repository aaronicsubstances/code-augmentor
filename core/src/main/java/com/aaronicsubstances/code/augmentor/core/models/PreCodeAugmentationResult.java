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

public class PreCodeAugmentationResult {

    public static class Header {
        @SerializedName("gen_code_start_suffix")
        String genCodeStartSuffix;
        @SerializedName("gen_code_end_suffix")
        String genCodeEndSuffix;
        @SerializedName("newline")
        String newline;
        @SerializedName("content_streaming_enabled")
        Boolean contentStreamEnabled;
    }

    private String genCodeStartSuffix;
    private String genCodeEndSuffix;
    private String newline;

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
        PersistenceUtil persistenceUtil= new PersistenceUtil(new PrintWriter(stream), closeStream);
        printHeader(persistenceUtil, true);
        return persistenceUtil;
    }

    private void printHeader(PersistenceUtil persistenceUtil, boolean contentStreamEnabled) 
            throws Exception {        
        Header header = new Header();
        header.genCodeStartSuffix = genCodeStartSuffix;
        header.genCodeEndSuffix = genCodeEndSuffix;
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

    public void serialize(File file) throws Exception {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer);
        }
    }

    public void serialize(Writer stream) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new PrintWriter(stream), true);
        try {
            printHeader(persistenceUtil, false);
            String json = PersistenceUtil.serializeFormattedToJson(fileDescriptors);
            persistenceUtil.println(json);
            persistenceUtil.flush();
        }
        finally {
            persistenceUtil.close();
        }
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
        PersistenceUtil persistenceUtil = new PersistenceUtil(new BufferedReader(stream), 
            closeStream);
        boolean contentStreamEnabled = readHeader(persistenceUtil);
        fileDescriptors = new ArrayList<>();
        if (!contentStreamEnabled) {            
            String inputRemainder = persistenceUtil.readToEnd();
            SourceFileDescriptor[] entireList = PersistenceUtil.deserializeFromJson(inputRemainder, 
                SourceFileDescriptor[].class);
            persistenceUtil.setContent(entireList);
        }
        return persistenceUtil;
    }

    private boolean readHeader(PersistenceUtil persistenceUtil) throws Exception {
        String headerString = persistenceUtil.readLine();
        Header header = PersistenceUtil.deserializeFromJson(headerString, Header.class);
        genCodeStartSuffix = header.genCodeStartSuffix;
        genCodeEndSuffix = header.genCodeEndSuffix;
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

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileDescriptors == null) ? 0 : fileDescriptors.hashCode());
        result = prime * result + ((genCodeEndSuffix == null) ? 0 : genCodeEndSuffix.hashCode());
        result = prime * result + ((genCodeStartSuffix == null) ? 0 : genCodeStartSuffix.hashCode());
        result = prime * result + ((newline == null) ? 0 : newline.hashCode());
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
        if (newline == null) {
            if (other.newline != null)
                return false;
        } else if (!newline.equals(other.newline))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PreCodeAugmentationResult{fileDescriptors=" + fileDescriptors + ", genCodeEndSuffix="
                + genCodeEndSuffix + ", genCodeStartSuffix=" + genCodeStartSuffix + ", newline=" + newline + "}";
    }
}