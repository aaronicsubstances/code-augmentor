package com.aaronicsubstances.code.augmentor.core.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;

/**
 * Represents contents of (OUTPUT|CHANGE)-SUMMARY.txt file contents in destination directory
 * used during completion stage. The contents are written
 * out using the default charset of the OS to make them accessible to OS shell scripts.
 */
public class CodeChangeSummary {

    /**
     * Represents group of 3 lines inside *-SUMMARY.txt file. Each line is a file
     * path using the native OS file path separators.
     */
    public static class ChangedFile {
        private String relativePath;
        private String srcDir;
        private String destDir;

        public ChangedFile(){

        }

        public ChangedFile(String relativePath, String srcDir, String destDir) {
            this.relativePath = relativePath;
            this.srcDir = srcDir;
            this.destDir = destDir;
        }

        public String getRelativePath() {
            return relativePath;
        }

        /**
         * Sets first of 3 lines for the common relative path of a source/generated file
         * pair represented by this object. Does not start with a file separator.
         * @param relativePath
         */
        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        public String getSrcDir() {
            return srcDir;
        }

        /**
         * Sets the second of 3 lines for the absolute path to base directory of source file whose
         * relative path is on the first line of the group of 3 lines represented by
         * this object.
         * @param srcDir
         */
        public void setSrcDir(String srcDir) {
            this.srcDir = srcDir;
        }

        public String getDestDir() {
            return destDir;
        }

        /**
         * Sets the third of 3 lines for the absolute path to the base directory of generated file whose
         * relative path is on the first line of the group of 3 lines represented by
         * this object.
         * @param destDir
         */
        public void setDestDir(String destDir) {
            this.destDir = destDir;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((destDir == null) ? 0 : destDir.hashCode());
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
            ChangedFile other = (ChangedFile) obj;
            if (destDir == null) {
                if (other.destDir != null)
                    return false;
            } else if (!destDir.equals(other.destDir))
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
            return "ChangedFile{destDir=" + destDir + ", relativePath=" + relativePath + ", srcDir=" + srcDir + "}";
        }

        public void serialize(Object serializer) throws Exception {
            PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
            persistenceUtil.println(relativePath);
            persistenceUtil.println(srcDir);
            persistenceUtil.println(destDir);
            persistenceUtil.flush();
        }

        public static ChangedFile deserialize(Object deserializer) throws Exception {
            PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
            String relativePath = persistenceUtil.readLine();
            String srcDir = persistenceUtil.readLine();
            String destDir = persistenceUtil.readLine();
            if (relativePath == null || srcDir == null || destDir == null) {
                return null;
            }
            return new ChangedFile(relativePath, srcDir, destDir);
        }
    }

    private List<ChangedFile> changedFiles;

    public CodeChangeSummary() {

    }

    public CodeChangeSummary(List<ChangedFile> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public Object beginSerialize(File file) throws Exception {
        // use OS platform default charset encoding for change summary 
        // since it is intended to be contain content valid for OS shell scripts.
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        return beginSerialize(writer, true);
    }

    public Object beginSerialize(Writer writer) throws Exception {
        return beginSerialize(writer, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) throws Exception {
        BufferedWriter writer = new BufferedWriter(stream);
        PersistenceUtil persistenceUtil= new PersistenceUtil(writer, closeStream);
        return persistenceUtil;
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = ((PersistenceUtil) serializer);
        try {
            persistenceUtil.flush();
        }
        finally {
            persistenceUtil.close();
        }
    }

    public void serialize(File file) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file))) {
            serialize(writer);
        }
    }

    public void serialize(Writer stream) throws Exception {
        Object serializer = beginSerialize(stream, false);
        for (ChangedFile cf : changedFiles) {
            cf.serialize(serializer);
        }
        endSerialize(serializer);
    }

    public Object beginDeserialize(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file));
        return beginDeserialize(reader, true);
    }

    public Object beginDeserialize(Reader reader) throws Exception {
        return beginDeserialize(reader, false);
    }

    private PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        BufferedReader reader = new BufferedReader(stream);
        PersistenceUtil persistenceUtil = new PersistenceUtil(reader, closeStream);
        changedFiles = new ArrayList<>(); 
        return persistenceUtil;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        persistenceUtil.close();
    }

    public static CodeChangeSummary deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file))) {
            return deserialize(reader);
        }
    }

    public static CodeChangeSummary deserialize(Reader reader) throws Exception {
        CodeChangeSummary instance = new CodeChangeSummary();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            ChangedFile cf;
            while ((cf = ChangedFile.deserialize(deserializer)) != null) {
                instance.changedFiles.add(cf);
            }
        }
        finally {
            instance.endDeserialize(deserializer);
        }
        return instance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changedFiles == null) ? 0 : changedFiles.hashCode());
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
        CodeChangeSummary other = (CodeChangeSummary) obj;
        if (changedFiles == null) {
            if (other.changedFiles != null)
                return false;
        } else if (!changedFiles.equals(other.changedFiles))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeChangeSummary{changedFiles=" + changedFiles + "}";
    }

    public List<ChangedFile> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<ChangedFile> changedFiles) {
        this.changedFiles = changedFiles;
    }
}