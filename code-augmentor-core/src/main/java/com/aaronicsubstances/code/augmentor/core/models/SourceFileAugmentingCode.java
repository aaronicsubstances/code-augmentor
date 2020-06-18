package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

/**
 * Represents a group of augmenting codes in a file identified by one set of augmenting
 * code directives.
 */
public class SourceFileAugmentingCode {
    private int fileId;
    private String dir;
    private String relativePath;
    private List<AugmentingCode> augmentingCodes;

    public SourceFileAugmentingCode() {
    }

    public SourceFileAugmentingCode(List<AugmentingCode> augmentingCodes) {
        this.augmentingCodes = augmentingCodes;
    }

    public int getFileId() {
        return fileId;
    }

    /**
     * Sets positive integer id which uniquely identifies file among all files passed to
     * preparation stage of Code Augmentor.
     * @param fileId
     */
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getDir() {
        return dir;
    }

    /**
     * Sets the base directory of the file set a file of augmenting codes belongs to.
     * @param dir
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Sets the path of a file of augmenting codes relative to base directory as defined by
     * {@link #getDir()}
     * @param relativePath
     */
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public List<AugmentingCode> getAugmentingCodes() {
        return augmentingCodes;
    }

    /**
     * Sets the augmenting codes of a file.
     * @param augmentingCodes
     */
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
        result = prime * result + fileId;
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
        if (fileId != other.fileId)
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
        return "SourceFileAugmentingCode{augmentingCodes=" + augmentingCodes + ", dir=" + dir + ", fileId="
                + fileId + ", relativePath=" + relativePath + "}";
    }
}