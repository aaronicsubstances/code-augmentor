package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.List;

public class AugCodeProcessingSpec {
    private List<String> dataDrivenDirectives;
    private List<String> uncheckedDirectives;
    private File destFile;

    public AugCodeProcessingSpec() {
    }

    public AugCodeProcessingSpec(List<String> dataDrivenDirectives, 
            List<String> uncheckedDirectives, File destFile) {
        this.dataDrivenDirectives = dataDrivenDirectives;
        this.uncheckedDirectives = uncheckedDirectives;
        this.destFile = destFile;
    }

    public List<String> getDataDrivenDirectives() {
        return dataDrivenDirectives;
    }

    public void setDataDrivenDirectives(List<String> dataDrivenDirectives) {
        this.dataDrivenDirectives = dataDrivenDirectives;
    }

    public List<String> getUncheckedDirectives() {
        return uncheckedDirectives;
    }

    public void setUncheckedDirectives(List<String> uncheckedDirectives) {
        this.uncheckedDirectives = uncheckedDirectives;
    }

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }
}