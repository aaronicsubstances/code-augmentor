package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class SourceFileChangeSet {
    private static final Gson JSON_CONVERT = new Gson();
    
    private int fileId;
    private String relativePath;
    private String srcDir;
    private String destDir;
    private List<CodeSnippetChangeDescriptor> changes;

    public SourceFileChangeSet() {        
    }

    public SourceFileChangeSet(List<CodeSnippetChangeDescriptor> changes) {
        this.changes = changes;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getSrcDir() {
        return srcDir;
    }

    public void setSrcDir(String srcDir) {
        this.srcDir = srcDir;
    }

    public String getDestDir() {
        return destDir;
    }

    public void setDestDir(String destDir) {
        this.destDir = destDir;
    }

    public List<CodeSnippetChangeDescriptor> getChanges() {
        return changes;
    }

    public void setChanges(List<CodeSnippetChangeDescriptor> changes) {
        this.changes = changes;
    }

    public void serialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        JSON_CONVERT.toJson(this, getClass(), persistenceUtil.getJsonWriter());
        persistenceUtil.flush();
    }

    public static SourceFileChangeSet deserialize(Object deserializer) throws Exception {
        JsonReader reader = ((PersistenceUtil) deserializer).getJsonReader();
        if (reader.peek() == JsonToken.END_ARRAY) {
            return null;
        }
        SourceFileChangeSet obj = JSON_CONVERT.fromJson(reader, SourceFileChangeSet.class);
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changes == null) ? 0 : changes.hashCode());
        result = prime * result + ((destDir == null) ? 0 : destDir.hashCode());
        result = prime * result + fileId;
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
        result = prime * result + ((srcDir == null) ? 0 : srcDir.hashCode());
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
        SourceFileChangeSet other = (SourceFileChangeSet) obj;
        if (changes == null) {
            if (other.changes != null)
                return false;
        } else if (!changes.equals(other.changes))
            return false;
        if (destDir == null) {
            if (other.destDir != null)
                return false;
        } else if (!destDir.equals(other.destDir))
            return false;
        if (fileId != other.fileId)
            return false;
        if (relativePath == null) {
            if (other.relativePath != null)
                return false;
        } else if (!relativePath.equals(other.relativePath))
            return false;
        if (srcDir == null) {
            if (other.srcDir != null)
                return false;
        } else if (!srcDir.equals(other.srcDir))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileChangeSet{changes=" + changes + ", destDir=" + destDir + ", fileId=" + fileId
                + ", relativePath=" + relativePath + ", srcDir=" + srcDir + "}";
    }
}