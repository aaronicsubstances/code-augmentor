package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class SourceFileDescriptor {
    private int fileIndex;
    private String dir;
    private String relativePath;
    private List<CodeSnippetDescriptor> codeSnippets;
    private String contentHash;

    public SourceFileDescriptor() {
    }

    public SourceFileDescriptor(List<CodeSnippetDescriptor> codeSnippets) {
        this.codeSnippets = codeSnippets;
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

    public List<CodeSnippetDescriptor> getCodeSnippets() {
        return codeSnippets;
    }

    public void setCodeSnippets(List<CodeSnippetDescriptor> codeSnippets) {
        this.codeSnippets = codeSnippets;
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
            // ignore blank lines.
            String json;
            while ((json = persistenceUtil.readLine()) != null) {
                if (!TaskUtils.isBlank(json)) {
                    break;
                }
            }
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
        result = prime * result + ((codeSnippets == null) ? 0 : codeSnippets.hashCode());
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
        if (codeSnippets == null) {
            if (other.codeSnippets != null)
                return false;
        } else if (!codeSnippets.equals(other.codeSnippets))
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
        return "SourceFileDescriptor{codeSnippets=" + codeSnippets + ", contentHash=" + contentHash + ", dir=" + dir
                + ", relativePath=" + relativePath + "}";
    }
}