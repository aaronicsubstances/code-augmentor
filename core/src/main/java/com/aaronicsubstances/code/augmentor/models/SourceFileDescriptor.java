package com.aaronicsubstances.code.augmentor.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class SourceFileDescriptor {
    @SerializedName("file_index")
    private int fileIndex;
    @SerializedName("dir")
    private String dir;
    @SerializedName("rel_path")
    private String relativePath;
    @SerializedName("imports")
    private List<String> importStatements;
    @SerializedName("header_insert_pos")
    private int headerInsertPos;
    @SerializedName("snippets")
    private List<CodeSnippetDescriptor> bodySnippets;
    private String contentHash;

    public SourceFileDescriptor() {
    }

    public SourceFileDescriptor(List<String> importStatements, 
            List<CodeSnippetDescriptor> bodySnippets) {
        this.importStatements = importStatements;
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

    public List<String> getImportStatements() {
        return importStatements;
    }

    public void setImportStatements(List<String> importStatements) {
        this.importStatements = importStatements;
    }

    public int getHeaderInsertPos() {
        return headerInsertPos;
    }

    public void setHeaderInsertPos(int headerInsertPos) {
        this.headerInsertPos = headerInsertPos;
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
        JsonWriter writer = (JsonWriter) serializer;
        TaskUtils.JSON_CONVERT.toJson(this, SourceFileDescriptor.class, writer);
        writer.flush();
    }

    public static SourceFileDescriptor deserialize(Object deserializer) throws Exception {
        JsonReader reader = (JsonReader) deserializer;
        if (reader.peek() == JsonToken.END_ARRAY) {
            return null;
        }
        SourceFileDescriptor obj = TaskUtils.JSON_CONVERT.fromJson(reader, SourceFileDescriptor.class);
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodySnippets == null) ? 0 : bodySnippets.hashCode());
        result = prime * result + ((contentHash == null) ? 0 : contentHash.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
        result = prime * result + headerInsertPos;
        result = prime * result + ((importStatements == null) ? 0 : importStatements.hashCode());
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
        if (headerInsertPos != other.headerInsertPos)
            return false;
        if (importStatements == null) {
            if (other.importStatements != null)
                return false;
        } else if (!importStatements.equals(other.importStatements))
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
                + ", headerInsertPos=" + headerInsertPos + ", importStatements=" + importStatements + ", relativePath="
                + relativePath + "}";
    }
}