package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

public class AugCodeSuffixSpec {

    @Parameter( required=true )
    private File destFile;

    @Parameter( required=true )
    private String[] suffixes;

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public String[] getSuffixes() {
        return suffixes;
    }

    public void setSuffixes(String[] suffixes) {
        this.suffixes = suffixes;
    }
}