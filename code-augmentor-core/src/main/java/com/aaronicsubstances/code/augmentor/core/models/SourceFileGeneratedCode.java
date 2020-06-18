package com.aaronicsubstances.code.augmentor.core.models;

import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

/**
 * Represents collection of generated codes for a source code file during
 * processing stage.
 */
public class SourceFileGeneratedCode {
    private int fileId;
    private List<GeneratedCode> generatedCodes;

    public SourceFileGeneratedCode() {

    }

    public SourceFileGeneratedCode(List<GeneratedCode> generatedCodes) {
        this.generatedCodes = generatedCodes;
    }
    
    public int getFileId() {
        return fileId;
    }

    /**
     * Sets the identifier of the source file whose augmenting codes led to
     * the generated codes contained in this object.
     * @param fileId
     */
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public List<GeneratedCode> getGeneratedCodes() {
        return generatedCodes;
    }

    /**
     * Sets the generated codes corresponding to augmenting codes
     * of a source file.
     * @param generatedCodes
     */
    public void setGeneratedCodes(List<GeneratedCode> generatedCodes) {
        this.generatedCodes = generatedCodes;
    }

    public void serialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        String json = PersistenceUtil.serializeCompactlyToJson(this);
        persistenceUtil.println(json);
        persistenceUtil.flush();
    }

    public static SourceFileGeneratedCode deserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        SourceFileGeneratedCode[] entireList = (SourceFileGeneratedCode[])persistenceUtil
            .getContent();
        SourceFileGeneratedCode obj = null;
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
                obj = PersistenceUtil.deserializeFromJson(json, SourceFileGeneratedCode.class);
            }
        }        
        return obj;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileId;
        result = prime * result + ((generatedCodes == null) ? 0 : generatedCodes.hashCode());
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
        if (fileId != other.fileId)
            return false;
        if (generatedCodes == null) {
            if (other.generatedCodes != null)
                return false;
        } else if (!generatedCodes.equals(other.generatedCodes))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileGeneratedCode{fileId=" + fileId + 
            ", generatedCodes=" + generatedCodes + "}";
    }
}