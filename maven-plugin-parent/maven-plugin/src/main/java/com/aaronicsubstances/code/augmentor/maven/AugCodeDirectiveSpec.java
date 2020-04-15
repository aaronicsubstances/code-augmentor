package com.aaronicsubstances.code.augmentor.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

public class AugCodeDirectiveSpec {

    @Parameter( required=true )
    private File destFile;

    @Parameter( required=true )
    private String[] directives;

    public File getDestFile() {
        return destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public String[] getDirectives() {
        return directives;
    }

    public void setDirectives(String[] directives) {
        this.directives = directives;
    }
}