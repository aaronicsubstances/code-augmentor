package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class SourceFileAugmentingCode {
    private int fileIndex;
    private String dir;
    private String relativePath;
    private List<AugmentingCode> augmentingCodes;

    public SourceFileAugmentingCode() {
    }

    public SourceFileAugmentingCode(List<AugmentingCode> augmentingCodes) {
        this.augmentingCodes = augmentingCodes;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public List<AugmentingCode> getAugmentingCodes() {
        return augmentingCodes;
    }

    public void setAugmentingCodes(List<AugmentingCode> augmentingCodes) {
        this.augmentingCodes = augmentingCodes;
    }

    public void serialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        String json = PersistenceUtil.serializeCompactlyToJson(this);
        persistenceUtil.println(json);
        persistenceUtil.flush();
    }

    public static SourceFileAugmentingCode deserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        SourceFileAugmentingCode[] entireList = (SourceFileAugmentingCode[])persistenceUtil
            .getContent();
        SourceFileAugmentingCode obj = null;
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
                obj = PersistenceUtil.deserializeFromJson(json, SourceFileAugmentingCode.class);
            }
        }        
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((augmentingCodes == null) ? 0 : augmentingCodes.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
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
        if (augmentingCodes == null) {
            if (other.augmentingCodes != null)
                return false;
        } else if (!augmentingCodes.equals(other.augmentingCodes))
            return false;
        if (dir == null) {
            if (other.dir != null)
                return false;
        } else if (!dir.equals(other.dir))
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
        return "SourceFileAugmentingCode{augmentingCodes=" + augmentingCodes + ", dir=" + dir + ", fileIndex="
                + fileIndex + ", relativePath=" + relativePath + "}";
    }
}