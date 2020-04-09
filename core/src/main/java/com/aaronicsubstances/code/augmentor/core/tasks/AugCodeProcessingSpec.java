package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.List;

public class AugCodeProcessingSpec {
    private File destFile;
    private List<String> directives;

    public AugCodeProcessingSpec() {
    }

    public AugCodeProcessingSpec(File destFile, List<String> directives) {
        this.destFile = destFile;
        this.directives = directives;
    }

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public void setDirectives(List<String> directives) {
        this.directives = directives;
    }
}