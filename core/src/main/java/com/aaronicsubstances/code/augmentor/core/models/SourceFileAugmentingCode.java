package com.aaronicsubstances.code.augmentor.core.models;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.persistence.PersistenceUtil;
import com.google.gson.annotations.SerializedName;

public class SourceFileAugmentingCode {
    @SerializedName("file_index")
    private int fileIndex;
    @SerializedName("rel_path")
    private String relativePath;
    @SerializedName("augmenting_codes")
    private List<AugmentingCode> augmentingCodeList;

    public SourceFileAugmentingCode() {
    }

    public SourceFileAugmentingCode(List<AugmentingCode> augmentingCodeList) {
        this.augmentingCodeList = augmentingCodeList;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public List<AugmentingCode> getAugmentingCodeList() {
        return augmentingCodeList;
    }

    public void setAugmentingCodeList(List<AugmentingCode> augmentingCodeList) {
        this.augmentingCodeList = augmentingCodeList;
    }

	public void serialize(Object serializer) throws Exception {
        PrintWriter writer = ((PersistenceUtil) serializer).getPrintWriter();
        String s = PersistenceUtil.serializeCompactlyToJson(this);
        writer.println(s);
    }

	public static SourceFileAugmentingCode deserialize(Object deserializer) throws Exception {
        BufferedReader reader = ((PersistenceUtil) deserializer).getBufferedReader();
        String line;
        while ((line = reader.readLine()) != null) {
            SourceFileAugmentingCode instance = PersistenceUtil.deserializeFromJson(line,
                SourceFileAugmentingCode.class);
            return instance;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((augmentingCodeList == null) ? 0 : augmentingCodeList.hashCode());
        result = prime * result + fileIndex;
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
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
        SourceFileAugmentingCode other = (SourceFileAugmentingCode) obj;
        if (augmentingCodeList == null) {
            if (other.augmentingCodeList != null)
                return false;
        } else if (!augmentingCodeList.equals(other.augmentingCodeList))
            return false;
        if (fileIndex != other.fileIndex)
            return false;
        if (relativePath == null) {
            if (other.relativePath != null)
                return false;
        } else if (!relativePath.equals(other.relativePath))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileAugmentingCode{augmentingCodeList=" + augmentingCodeList +
                ", fileIndex=" + fileIndex +
                ", relativePath=" + relativePath + "}";
    }
}