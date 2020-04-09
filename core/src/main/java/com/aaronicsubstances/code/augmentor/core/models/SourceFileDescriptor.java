package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.google.gson.annotations.SerializedName;

public class SourceFileDescriptor {
    @SerializedName("file_index")
    private int fileIndex;
    @SerializedName("dir")
    private String dir;
    @SerializedName("rel_path")
    private String relativePath;
    @SerializedName("snippets")
    private List<CodeSnippetDescriptor> bodySnippets;
    private String contentHash;

    public SourceFileDescriptor() {
    }

    public SourceFileDescriptor(List<CodeSnippetDescriptor> bodySnippets) {
        this.bodySnippets = bodySnippets;
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

    public List<CodeSnippetDescriptor> getBodySnippets() {
        return bodySnippets;
    }

    public void setBodySnippets(List<CodeSnippetDescriptor> bodySnippets) {
        this.bodySnippets = bodySnippets;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public void serialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        String json = PersistenceUtil.serializeCompactlyToJson(this);
        persistenceUtil.println(json);
        persistenceUtil.flush();
    }

    public static SourceFileDescriptor deserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        SourceFileDescriptor[] entireList = (SourceFileDescriptor[])persistenceUtil
            .getContent();
        SourceFileDescriptor obj = null;
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
                obj = PersistenceUtil.deserializeFromJson(json, SourceFileDescriptor.class);
            }
        }        
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodySnippets == null) ? 0 : bodySnippets.hashCode());
        result = prime * result + ((contentHash == null) ? 0 : contentHash.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
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
        SourceFileDescriptor other = (SourceFileDescriptor) obj;
        if (bodySnippets == null) {
            if (other.bodySnippets != null)
                return false;
        } else if (!bodySnippets.equals(other.bodySnippets))
            return false;
        if (contentHash == null) {
            if (other.contentHash != null)
                return false;
        } else if (!contentHash.equals(other.contentHash))
            return false;
        if (dir == null) {
            if (other.dir != null)
                return false;
        } else if (!dir.equals(other.dir))
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
        return "SourceFileDescriptor{bodySnippets=" + bodySnippets + ", contentHash=" + contentHash + ", dir=" + dir
                + ", relativePath=" + relativePath + "}";
    }
}