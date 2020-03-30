package com.aaronicsubstances.code.augmentor.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;

public class SourceFileAugmentingCode {
    private int fileIndex;
    private String relativePath;
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
        String s = TaskUtils.serializeToJson(this);
        byte[] buf = s.getBytes(StandardCharsets.UTF_8);
        ZipOutputStream zip = (ZipOutputStream) serializer;
        ZipEntry e = new ZipEntry(fileIndex + ".json");
        zip.putNextEntry(e);
        ByteArrayInputStream inStream = new ByteArrayInputStream(buf);
        TaskUtils.copyStream(inStream, zip);
        zip.closeEntry();
    }

    public static SourceFileAugmentingCode deserialize(Object deserializer) throws Exception {
        ZipInputStream zip = (ZipInputStream) deserializer;
        ZipEntry e = zip.getNextEntry();
        if (e == null) {
            return null;
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        TaskUtils.copyStream(zip, outStream);
        outStream.flush();
        zip.closeEntry();
        byte[] buf = outStream.toByteArray();
        String s = new String(buf, StandardCharsets.UTF_8);
        SourceFileAugmentingCode obj = TaskUtils.deserializeFromJson(s, 
            SourceFileAugmentingCode.class);
        return obj;
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