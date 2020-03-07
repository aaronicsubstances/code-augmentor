package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

/**
 * 
 */
public class CodeGenerationRequestSpecification {
    private static final Pattern WS_REGEX = Pattern.compile("(?s)\\s");

    // use extension to determine which of XML or qCSV formats to use.
    private File augmentingCodeDestFile; 

    private final List<String> startSuffixes = new ArrayList<>();
    private final List<String> continuationDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> closingDoubleSlashSuffixes = new ArrayList<>();

    public static String validateCommentMarkerSuffix(String suffix) {
        // Ensure suffix is neither empty nor contains whitespace.
        suffix = suffix.trim();
        if (suffix.isEmpty()) {
            throw new BuildException("Comment marker suffix cannot be empty");
        }
        if (WS_REGEX.matcher(suffix).find()) {
            throw new BuildException("Whitespace characters not acceptable in comment marker suffix");
        }
        return suffix;
    }
    
    public File getAugmentingCodeDestFile() {
        return augmentingCodeDestFile;
    }

    public void setAug_code_file(File f) {
        this.augmentingCodeDestFile = f;
    }

    public List<String> getStartSuffixes() {
        return startSuffixes;
    }

    public List<String> getContinuationDoubleSlashSuffixes() {
        return continuationDoubleSlashSuffixes;
    }

    public List<String> getClosingDoubleSlashSuffixes() {
        return closingDoubleSlashSuffixes;
    }

    public void addStart_suffix(String suffix) {
        suffix = validateCommentMarkerSuffix(suffix);
        if (!startSuffixes.contains(suffix)) {
            startSuffixes.add(suffix);
        }
    }

    public void addContinuation_dslash_suffix(String suffix) {
        suffix = validateCommentMarkerSuffix(suffix);
        if (!continuationDoubleSlashSuffixes.contains(suffix)) {
            continuationDoubleSlashSuffixes.add(suffix);
        }
    }

    public void addClosing_dslash_suffix(String suffix) {
        suffix = validateCommentMarkerSuffix(suffix);
        if (!closingDoubleSlashSuffixes.contains(suffix)) {
            closingDoubleSlashSuffixes.add(suffix);
        }
    }

	public void validate() {
        if (augmentingCodeDestFile == null) {
            throw new BuildException("spec[@aug_code_file] attribute is required");
        }
        if (startSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested spec/start_suffix element is required");
        }
        if (continuationDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested spec/continuation_dslash_suffix element is required");
        }
        if (closingDoubleSlashSuffixes.isEmpty()) {
            throw new BuildException("at least 1 nested spec/closing_dslash_suffix element is required");
        }
	}
}