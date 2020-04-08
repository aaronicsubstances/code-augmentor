package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.persistence.PersistenceUtil;
import com.google.gson.annotations.SerializedName;

public class SourceFileGeneratedCode {
    @SerializedName("file_index")
    private int fileIndex;
    @SerializedName("newline")
    private String newline;
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

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public List<GeneratedCode> getGeneratedCodeList() {
        return generatedCodeList;
    }

    public void setGeneratedCodeList(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
    }

    public void serialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        String json = PersistenceUtil.serializeCompactlyToJson(this);
        persistenceUtil.println(json);
        persistenceUtil.flush();
    }

    public static SourceFileGeneratedCode deserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        SourceFileGeneratedCode[] entireList = (SourceFileGeneratedCode[])persistenceUtil
            .getContent();
        SourceFileGeneratedCode obj = null;
        if (entireList != null) {
            int contentIndex = persistenceUtil.getContentIndex();
            if (contentIndex < entireList.length) {
                obj = entireList[contentIndex];
                persistenceUtil.setContentIndex(contentIndex + 1);
            }
        }
        else {
            String json = persistenceUtil.readLine();
            if (json != null) {
                obj = PersistenceUtil.deserializeFromJson(json, SourceFileGeneratedCode.class);
            }
        }        
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileIndex;
        result = prime * result + ((generatedCodeList == null) ? 0 : generatedCodeList.hashCode());
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
        SourceFileGeneratedCode other = (SourceFileGeneratedCode) obj;
        if (fileIndex != other.fileIndex)
            return false;
        if (generatedCodeList == null) {
            if (other.generatedCodeList != null)
                return false;
        } else if (!generatedCodeList.equals(other.generatedCodeList))
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
        return "SourceFileGeneratedCode{fileIndex=" + fileIndex + ", generatedCodeList=" + generatedCodeList
                + ", newline=" + newline + "}";
    }
}