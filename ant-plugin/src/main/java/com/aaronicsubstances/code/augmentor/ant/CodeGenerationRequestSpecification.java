package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.parsing.LexerSupport;

import org.apache.tools.ant.BuildException;

public class CodeGenerationRequestSpecification {
    private File augCodeDestFile; 

    private final List<SuffixSpec> augCodeSuffixes;

    public static SuffixSpec validateCommentMarkerSuffix(SuffixSpec suffix) {
        if (suffix.getValue() == null || suffix.getValue().isEmpty()) {
            throw new BuildException("value attribute not specified.");
        }
        // Ensure suffix does not contain newlines.
        if (LexerSupport.NEW_LINE_REGEX.matcher(suffix.getValue()).find()) {
            throw new BuildException("Newline characters not acceptable in comment marker suffix");
        }
        return suffix;
    }

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
        suffix = validateCommentMarkerSuffix(suffix);
        if (!augCodeSuffixes.contains(suffix)) {
            augCodeSuffixes.add(suffix);
        }
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