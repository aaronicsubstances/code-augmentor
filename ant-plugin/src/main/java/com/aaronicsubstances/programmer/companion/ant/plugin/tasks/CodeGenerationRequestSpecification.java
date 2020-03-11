package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.aaronicsubstances.programmer.companion.LexerSupport;

import org.apache.tools.ant.BuildException;

/**
 * 
 */
public class CodeGenerationRequestSpecification {
    private static final Pattern WS_REGEX = Pattern.compile("(?s)\\s");

    // use extension to determine which of XML or qCSV formats to use.
    private File augCodeDestFile; 

    private final List<String> augCodeSuffixes = new ArrayList<>();

    public static String validateCommentMarkerSuffix(String suffix) {
        // Ensure suffix does not contain newlines.
        if (LexerSupport.NEW_LINE_REGEX.matcher(suffix).find()) {
            throw new BuildException("Newline characters not acceptable in comment marker suffix");
        }
        return suffix;
    }
    
    public File getAugCodeDestFile() {
        return augCodeDestFile;
    }

    public void setAug_code_file(File f) {
        this.augCodeDestFile = f;
    }

    public List<String> getAugCodeSuffixes() {
        return augCodeSuffixes;
    }

    public void addAug_code_suffix(String suffix) {
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