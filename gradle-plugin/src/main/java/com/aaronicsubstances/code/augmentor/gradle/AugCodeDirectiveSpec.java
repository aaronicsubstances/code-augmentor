package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.util.List;

public class AugCodeDirectiveSpec {

    private File destFile;

    private List<String> suffixes;

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public List<String> getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(List<String> suffixes) {
        this.suffixes = suffixes;
    }
}