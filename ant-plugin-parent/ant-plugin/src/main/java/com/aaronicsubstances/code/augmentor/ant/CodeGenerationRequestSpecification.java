package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

public class CodeGenerationRequestSpecification {
    private File augCodeDestFile; 

    private final List<SuffixSpec> augCodeSuffixes;

    public CodeGenerationRequestSpecification() {
        augCodeSuffixes = new ArrayList<>();
    }

    CodeGenerationRequestSpecification(List<SuffixSpec> augCodeSuffixes) {
        this.augCodeSuffixes = augCodeSuffixes;
    }
    
    public File getAugCodeDestFile() {
        return augCodeDestFile;
    }

    public void setAug_code_file(File f) {
        this.augCodeDestFile = f;
    }

    public List<SuffixSpec> getAugCodeSuffixes() {
        return augCodeSuffixes;
    }

    public void addConfiguredAug_code_suffix(SuffixSpec suffix) {
        if (suffix.getValue() == null) {            
            throw new BuildException("spec/aug_code_suffix[@value] attribute not specified.");
        }
        augCodeSuffixes.add(suffix);
    }

    public void validate() {
        if (augCodeDestFile == null) {
            throw new BuildException("spec[@aug_code_file] attribute is required");
        }
        if (augCodeSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested spec/aug_code_suffix element is required");
        }
    }
}