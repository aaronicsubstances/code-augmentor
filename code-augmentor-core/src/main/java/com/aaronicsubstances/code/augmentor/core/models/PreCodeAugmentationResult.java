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

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;

public class PreCodeAugmentationResult {

    public static class Header {
        String genCodeStartDirective;
        String genCodeEndDirective;
        Boolean contentStreamingEnabled;
    }

    private String genCodeStartDirective;
    private String genCodeEndDirective;

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

    public String getGenCodeStartDirective() {
        return genCodeStartDirective;
    }

    public void setGenCodeStartDirective(String genCodeStartDirective) {
        this.genCodeStartDirective = genCodeStartDirective;
    }

    public String getGenCodeEndDirective() {
        return genCodeEndDirective;
    }

    public void setGenCodeEndDirective(String genCodeEndDirective) {
        this.genCodeEndDirective = genCodeEndDirective;
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
        header.genCodeStartDirective = genCodeStartDirective;
        header.genCodeEndDirective = genCodeEndDirective;
        // try not to set contentStreamEnabled to true since it's true by default.
        if (!contentStreamEnabled) {
            header.contentStreamingEnabled = false;
        }
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
        boolean contentStreamingEnabled = readHeader(persistenceUtil);
        fileDescriptors = new ArrayList<>();
        if (!contentStreamingEnabled) {            
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
        genCodeStartDirective = header.genCodeStartDirective;
        genCodeEndDirective = header.genCodeEndDirective;
        // enable content streaming by default.
        boolean contentStreamingEnabled = true;
        if (header.contentStreamingEnabled != null) {
            contentStreamingEnabled = header.contentStreamingEnabled;
        }
        return contentStreamingEnabled;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileDescriptors == null) ? 0 : fileDescriptors.hashCode());
        result = prime * result + ((genCodeEndDirective == null) ? 0 : genCodeEndDirective.hashCode());
        result = prime * result + ((genCodeStartDirective == null) ? 0 : genCodeStartDirective.hashCode());
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
        if (genCodeEndDirective == null) {
            if (other.genCodeEndDirective != null)
                return false;
        } else if (!genCodeEndDirective.equals(other.genCodeEndDirective))
            return false;
        if (genCodeStartDirective == null) {
            if (other.genCodeStartDirective != null)
                return false;
        } else if (!genCodeStartDirective.equals(other.genCodeStartDirective))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PreCodeAugmentationResult{fileDescriptors=" + fileDescriptors + ", genCodeEndDirective="
                + genCodeEndDirective + ", genCodeStartDirective=" + genCodeStartDirective + "}";
    }
}