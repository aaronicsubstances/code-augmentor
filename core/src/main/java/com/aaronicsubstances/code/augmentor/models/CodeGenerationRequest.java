package com.aaronicsubstances.code.augmentor.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;

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

    public static CodeGenerationRequest deserialize(String str) throws Exception {
        SourceFileAugmentingCode[] fileAugCodes = TaskUtils.deserializeFromJson(str,
            SourceFileAugmentingCode[].class);
        CodeGenerationRequest instance = new CodeGenerationRequest();
        instance.setSourceFileAugmentingCodeList(Arrays.asList(fileAugCodes));
        return instance;
    }

    public static CodeGenerationRequest deserialize(File file) throws Exception {
        CodeGenerationRequest instance = new CodeGenerationRequest(new ArrayList<>());
        Object deserializer = null;
        try {
            deserializer = beginDeserialize(file);
            SourceFileAugmentingCode s;
            while ((s = SourceFileAugmentingCode.deserialize(deserializer)) != null) {
                instance.getSourceFileAugmentingCodeList().add(s);
            }
        }
        finally {
            endDeserialize(deserializer);
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