package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;

public class ProcessCodeContext {
    private final Map<String, Object> globalScope = new HashMap<>();
    private final Map<String, Object> fileScope = new HashMap<>();
    private SourceFileAugmentingCode fileAugCodes;
    private int augCodeIndex;
    private File srcFile;

    // Intended for use by Groovy scripts
    public GeneratedCode newGenCode() {
        return new GeneratedCode(new ArrayList<>());
    }

    // Intended for use by Groovy scripts
    public ContentPart newContent(String content) {
        return new ContentPart(content, false);
    }

    // Intended for use by Groovy scripts
    public ContentPart newContent(String content, boolean exactMatch) {
        return new ContentPart(content, exactMatch);
    }

    public Map<String, Object> getGlobalScope() {
        return globalScope;
    }

    public Map<String, Object> getFileScope() {
        return fileScope;
    }

    public SourceFileAugmentingCode getFileAugCodes() {
        return fileAugCodes;
    }

    void setFileAugCodes(SourceFileAugmentingCode fileAugCodes) {
        this.fileAugCodes = fileAugCodes;
    }

    public int getAugCodeIndex() {
        return augCodeIndex;
    }

    void setAugCodeIndex(int augCodeIndex) {
        this.augCodeIndex = augCodeIndex;
    }

    public File getSrcFile() {
        return srcFile;
    }

    void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
}