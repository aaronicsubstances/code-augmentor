package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class SourceFileGeneratedCode {
    private int fileIndex;
    private String newline;
    private List<GeneratedCode> generatedCodes;

    public SourceFileGeneratedCode() {

    }

    public SourceFileGeneratedCode(List<GeneratedCode> generatedCodes) {
        this.generatedCodes = generatedCodes;
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

    public List<GeneratedCode> getGeneratedCodes() {
        return generatedCodes;
    }

    public void setGeneratedCodes(List<GeneratedCode> generatedCodes) {
        this.generatedCodes = generatedCodes;
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
            // ignore blank lines.
            String json;
            while ((json = persistenceUtil.readLine()) != null) {
                if (!TaskUtils.isBlank(json)) {
                    break;
                }
            }
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
        result = prime * result + ((generatedCodes == null) ? 0 : generatedCodes.hashCode());
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
        if (generatedCodes == null) {
            if (other.generatedCodes != null)
                return false;
        } else if (!generatedCodes.equals(other.generatedCodes))
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
        return "SourceFileGeneratedCode{fileIndex=" + fileIndex + ", generatedCodes=" + generatedCodes
                + ", newline=" + newline + "}";
    }
}