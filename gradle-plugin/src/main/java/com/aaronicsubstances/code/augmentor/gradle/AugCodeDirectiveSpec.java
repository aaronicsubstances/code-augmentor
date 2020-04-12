package com.aaronicsubstances.code.augmentor.gradle;

import java.util.List;

import org.gradle.api.file.RegularFile;

public class AugCodeDirectiveSpec {
    private RegularFile destFile;
    private List<String> directives;

    public RegularFile getDestFile() {
        return destFile;
    }

    public void setDestFile(RegularFile destFile) {
        this.destFile = destFile;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public void setDirectives(List<String> directives) {
        this.directives = directives;
    }
}