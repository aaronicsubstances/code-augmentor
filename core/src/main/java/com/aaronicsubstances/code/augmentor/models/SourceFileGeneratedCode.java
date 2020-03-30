package com.aaronicsubstances.code.augmentor.models;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.aaronicsubstances.code.augmentor.tasks.TaskUtils;

public class SourceFileGeneratedCode {
    private int fileIndex;
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

    public List<GeneratedCode> getGeneratedCodeList() {
        return generatedCodeList;
    }

    public void setGeneratedCodeList(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
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

	public static SourceFileGeneratedCode deserialize(Object deserializer) throws Exception {
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
        SourceFileGeneratedCode obj = TaskUtils.deserializeFromJson(s, 
            SourceFileGeneratedCode.class);
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileIndex;
        result = prime * result + ((generatedCodeList == null) ? 0 : generatedCodeList.hashCode());
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
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileGeneratedCode{fileIndex=" + fileIndex 
            + ", generatedCodeList=" + generatedCodeList + "}";
    }
}