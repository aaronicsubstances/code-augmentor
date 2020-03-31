package com.aaronicsubstances.code.augmentor.models;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;
import com.google.gson.annotations.SerializedName;

public class SourceFileGeneratedCode {
    @SerializedName("file_index")
    private int fileIndex;
    @SerializedName("generated_codes")
    private List<GeneratedCode> generatedCodeList;

    public SourceFileGeneratedCode() {

    }

    public SourceFileGeneratedCode(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
    }
    
    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public List<GeneratedCode> getGeneratedCodeList() {
        return generatedCodeList;
    }

    public void setGeneratedCodeList(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
    }

	public void serialize(Object serializer) throws Exception {
        PrintWriter writer = (PrintWriter) serializer;
        String s = TaskUtils.serializeCompactlyToJson(this);
        writer.println(s);
    }

	public static SourceFileGeneratedCode deserialize(Object deserializer) throws Exception {
        BufferedReader reader = (BufferedReader) deserializer;
        String line;
        while ((line = reader.readLine()) != null) {
            // ignore comments and blank lines.
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            SourceFileGeneratedCode instance = TaskUtils.deserializeFromJson(line,
                SourceFileGeneratedCode.class);
            return instance;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileIndex;
        result = prime * result + ((generatedCodeList == null) ? 0 : generatedCodeList.hashCode());
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
        SourceFileGeneratedCode other = (SourceFileGeneratedCode) obj;
        if (fileIndex != other.fileIndex)
            return false;
        if (generatedCodeList == null) {
            if (other.generatedCodeList != null)
                return false;
        } else if (!generatedCodeList.equals(other.generatedCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileGeneratedCode{fileIndex=" + fileIndex 
            + ", generatedCodeList=" + generatedCodeList + "}";
    }
}