package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class CodeGenerationRequestSpecification {
    // use extension to determine which of XML or qCSV formats to use.
    private File augmentingCodeDestFile; 

    private final List<String> startSuffixes = new ArrayList<>();
    private final List<String> continuationDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> closingDoubleSlashSuffixes = new ArrayList<>();

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
        startSuffixes.add(suffix);
    }

    public void addContinuation_dslash_suffix(String suffix) {
        continuationDoubleSlashSuffixes.add(suffix);
    }

    public void addClosing_dslash_suffix(String suffix) {
        closingDoubleSlashSuffixes.add(suffix);
    }

	public void validate() {
	}
}