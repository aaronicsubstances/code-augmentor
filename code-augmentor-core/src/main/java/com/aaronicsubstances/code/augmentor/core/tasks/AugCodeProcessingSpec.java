package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.List;

/**
 * Defines aug code processing directives and the destination file for augmenting code sections
 * identified with those directives.
 */
public class AugCodeProcessingSpec {
    private File destFile;
    private List<String> directives;

    public AugCodeProcessingSpec() {
    }

    public AugCodeProcessingSpec(File destFile, List<String> directives) {
        this.destFile = destFile;
        this.directives = directives;
    }

    public File getDestFile() {
        return destFile;
    }

    /**
     * Sets the file for storing augmenting code associated with this 
     * instance's directives. This will be used as input to the processing stage.
     * @param destFile
     * 
     * @see ProcessCodeGenericTask#setInputFile(File)
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public List<String> getDirectives() {
        return directives;
    }

    /**
     * Sets the directives used to identify augmenting codes. 
     * @param directives list of directive markers. Only markers with a non-whitespace
     * character will actually be used.
     */
    public void setDirectives(List<String> directives) {
        this.directives = directives;
    }
}