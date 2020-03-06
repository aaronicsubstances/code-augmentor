package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class CodeGenerationRequestSpecification {
    // use extension to determine which of XML or qCSV formats to use.
    private File aug_code_file; 

    private final List<String> headerDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> bodyStartSlashStarSuffixes = new ArrayList<>();
    private final List<String> bodyStartDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> bodyContinuationDoubleSlashSuffixes = new ArrayList<>();
    private final List<String> bodyEndDoubleSlashSuffixes = new ArrayList<>();

    public File getAug_code_file() {
        return aug_code_file;
    }

    public void setAug_code_file(File aug_code_file) {
        this.aug_code_file = aug_code_file;
    }

    public void addBody_end_dslash_suffix(final String suffix) {
        bodyEndDoubleSlashSuffixes.add(suffix);
    }

    public List<String> getHeaderDoubleSlashSuffixes() {
        return headerDoubleSlashSuffixes;
    }

    public List<String> getBodyStartSlashStarSuffixes() {
        return bodyStartSlashStarSuffixes;
    }

    public List<String> getBodyStartDoubleSlashSuffixes() {
        return bodyStartDoubleSlashSuffixes;
    }

    public List<String> getBodyContinuationDoubleSlashSuffixes() {
        return bodyContinuationDoubleSlashSuffixes;
    }

    public List<String> getBodyEndDoubleSlashSuffixes() {
        return bodyEndDoubleSlashSuffixes;
    }

    public void addHeader_dslash_suffix(final String suffix) {
        headerDoubleSlashSuffixes.add(suffix);
    }

    public void addBody_start_sstar_suffix(final String suffix) {
        bodyStartSlashStarSuffixes.add(suffix);
    }

    public void addBody_start_dslash_suffix(final String suffix) {
        bodyStartDoubleSlashSuffixes.add(suffix);
    }

    public void addBody_cont_dslash_suffix(final String suffix) {
        bodyContinuationDoubleSlashSuffixes.add(suffix);
    }
}