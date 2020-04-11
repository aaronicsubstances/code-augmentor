package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

public class CodeGenerationRequestSpecification {
    private File augCodeDestFile;
    private final List<Directive> augCodeDirectives = new ArrayList<>();
    
    public File getAugCodeDestFile() {
        return augCodeDestFile;
    }

    public void setAug_code_file(File f) {
        this.augCodeDestFile = f;
    }

    public List<Directive> getAugCodeDirectives() {
        return augCodeDirectives;
    }

    public void addConfiguredAug_code_directive(Directive d) {
        if (d.getValue() == null) {            
            throw new BuildException("spec/aug_code_directive[@value] attribute not specified.");
        }
        augCodeDirectives.add(d);
    }

    public void validate() {
        if (augCodeDestFile == null) {
            throw new BuildException("spec[@aug_code_file] attribute is required");
        }
        if (augCodeDirectives.isEmpty()) {
            throw new BuildException("at least 1 nested spec/aug_code_directive element is required");
        }
    }
}