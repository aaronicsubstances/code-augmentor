package com.aaronicsubstances.code.augmentor.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;

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
        FileOutputStream fout = new FileOutputStream(file);
        ZipOutputStream zip = new ZipOutputStream(fout, StandardCharsets.UTF_8);
        return zip;
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer == null) {
            return;
        }
        ZipOutputStream zip = (ZipOutputStream) serializer;
        zip.finish();
    }

    public static Object beginDeserialize(File file) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        ZipInputStream zip = new ZipInputStream(fin, StandardCharsets.UTF_8);
        return zip;
    }

    public static void endDeserialize(Object deserializer) throws Exception {
        if (deserializer == null) {
            return;
        }
        ZipInputStream zip = (ZipInputStream) deserializer;
        zip.close();
    }

    public static CodeGenerationResponse deserialize(String str) throws Exception {
        SourceFileGeneratedCode[] fileGenCodes = TaskUtils.deserializeFromJson(str,
            SourceFileGeneratedCode[].class);
        CodeGenerationResponse instance = new CodeGenerationResponse();
        instance.setSourceFileGeneratedCodeList(Arrays.asList(fileGenCodes));
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