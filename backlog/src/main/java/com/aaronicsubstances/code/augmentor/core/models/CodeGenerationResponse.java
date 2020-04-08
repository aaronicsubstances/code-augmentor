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

public class CodeGenerationResponse {
    private List<SourceFileGeneratedCode> sourceFileGeneratedCodeList;

    public CodeGenerationResponse() {
    }

    public CodeGenerationResponse(List<SourceFileGeneratedCode> sourceFileGeneratedCodeList) {
        this.sourceFileGeneratedCodeList = sourceFileGeneratedCodeList;
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

    public Object beginSerialize(Writer writer) {
        return beginSerialize(writer, false);
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
            String json = PersistenceUtil.serializeToJson(sourceFileGeneratedCodeList);
            stream.write(json);
            stream.flush();
        }
        else {
            PrintWriter writer = new PrintWriter(stream);
            PersistenceUtil persistenceUtil = new PersistenceUtil(writer, false);
            for (SourceFileGeneratedCode s : sourceFileGeneratedCodeList) {
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

    private PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        BufferedReader reader = new BufferedReader(stream);
        boolean perFile = PersistenceUtil.peekSerializedJsonForPerFile(reader);
        if (perFile) {
            SourceFileGeneratedCode[] files = PersistenceUtil.deserializeFromJson(reader,
                SourceFileGeneratedCode[].class);
            sourceFileGeneratedCodeList = Arrays.asList(files);
        }
        else {
            sourceFileGeneratedCodeList = new ArrayList<>();
        }

        return new PersistenceUtil(reader, closeStream);
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        BufferedReader reader = persistenceUtil.getBufferedReader();
        if (persistenceUtil.isCloseWhenDone()) {
            reader.close();
        }
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
        if (sourceFileGeneratedCodeList == null) {
            if (other.sourceFileGeneratedCodeList != null)
                return false;
        } else if (!sourceFileGeneratedCodeList.equals(other.sourceFileGeneratedCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationResponse{sourceFileGeneratedCodeList=" + sourceFileGeneratedCodeList + "}";
    }
}