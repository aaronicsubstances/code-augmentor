package com.aaronicsubstances.code.augmentor.core.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;

public class ProcessCodeContext {
    private final Map<String, Object> globalScope = new HashMap<>();
    private final Map<String, Object> fileScope = new HashMap<>();
    private SourceFileAugmentingCode fileAugCodes;
    private int augCodeIndex;

    // used by Groovy script.
    public GeneratedCode newGenCode() {
        return new GeneratedCode(new ArrayList<>());
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
}