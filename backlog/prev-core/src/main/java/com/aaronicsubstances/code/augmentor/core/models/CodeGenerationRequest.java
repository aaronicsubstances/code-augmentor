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
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.persistence.PersistenceUtil;

public class CodeGenerationRequest {
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

    public Object beginSerialize(Writer stream) {
        return beginSerialize(stream, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) {
        PrintWriter writer = new PrintWriter(stream, true);
        return new PersistenceUtil(writer, closeStream);
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        PrintWriter writer = persistenceUtil.getPrintWriter();
        try {
            writer.flush();
        }
        finally {
            if (persistenceUtil.isCloseWhenDone()) {
                writer.close();
            }
        }
    }

    public void serialize(File file, boolean serializeAllAsJson) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer, serializeAllAsJson);
        }
    }

    public void serialize(Writer stream, boolean serializeAllAsJson) throws Exception {
        if (serializeAllAsJson) {
            String json = PersistenceUtil.serializeToJson(sourceFileAugmentingCodeList);
            stream.write(json);
            stream.flush();
        }
        else {
            PrintWriter writer = new PrintWriter(stream);
            PersistenceUtil persistenceUtil = new PersistenceUtil(writer, false);
            for (SourceFileAugmentingCode s : sourceFileAugmentingCodeList) {
                s.serialize(persistenceUtil);
            }
            writer.flush();
        }
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
        BufferedReader reader = new BufferedReader(stream);
        boolean perFile = PersistenceUtil.peekSerializedJsonForPerFile(reader);
        if (perFile) {
            SourceFileAugmentingCode[] files = PersistenceUtil.deserializeFromJson(reader,
                SourceFileAugmentingCode[].class);
            sourceFileAugmentingCodeList = Arrays.asList(files);
        }
        else {
            sourceFileAugmentingCodeList = new ArrayList<>();
        }

        return new PersistenceUtil(reader, closeStream);
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        Reader reader = persistenceUtil.getBufferedReader();
        if (persistenceUtil.isCloseWhenDone()) {
            reader.close();
        }
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