package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;

public class CodeGenerationResponseSpecification {
    private File genCodeFile;

    public File getGenCodeFile() {
        return genCodeFile;
    }

    public void setGen_code_file(File f) {
        this.genCodeFile = f;
    }

    public void validate() {
        if (genCodeFile == null) {
            throw new BuildException("spec[@gen_code_file] attribute is required");
        }
    }    
}