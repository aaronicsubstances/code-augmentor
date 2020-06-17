package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

/**
 * Helper context object intended for use by scripts written in Groovy (and other JVM languages) during
 * processing stage of Code Augmentor.
 */
public class ProcessCodeContext {
    public static final String RESERVED_VAR_NAME_PREFIX = "codeAugmentor.";

    private Object header;
    private final Map<String, Object> globalScope = new HashMap<>();
    private final Map<String, Object> fileScope = new HashMap<>();
    private SourceFileAugmentingCode fileAugCodes;
    private int augCodeIndex;
    private File srcFile;

    public ProcessCodeContext() {
        globalScope.put(RESERVED_VAR_NAME_PREFIX + "indent", TaskUtils.strMultiply(" ", 4));
    }

    /**
     * Creates a new {@link GeneratedCode} with an empty modifiable list of
     * content parts.  
     * @return new generated code object.
     */
    public GeneratedCode newGenCode() {
        return new GeneratedCode(new ArrayList<>());
    }

    /**
     * Creates a new content part with inexact matching.
     * @param content string of content part
     * @return new content part with inexact matching.
     */
    public ContentPart newContent(String content) {
        return new ContentPart(content, false);
    }

    /**
     * Creates a new content part.
     * @param content string of content part.
     * @param exactMatch true to match string exactly and disallow insertion of
     * indentation; false to admit insertion of leading indents.
     * @return new content part.
     */
    public ContentPart newContent(String content, boolean exactMatch) {
        return new ContentPart(content, exactMatch);
    }

    /**
     * Returns result of parsing the header (ie first line) of
     * the files returned by preparation stage as aug code files
     * and presented separately to processing stage as input files.
     * @return parsed header from input file to processing stage.
     */
	public Object getHeader() {
        return header;
	}

	public void setHeader(Object header) {
        this.header = header;
	}

    /**
     * Returns readonly map for use by JVM language script during
     * processing stage to record global objects for its own purposes.
     * Contents of this map stay put throughout processing stage.
     * @return global scope dictionary.
     */
    public Map<String, Object> getGlobalScope() {
        return globalScope;
    }

    /**
     * Returns readonly map for use by JVM language script during
     * processing stage to record objects for a file of augmenting code objects 
     * for script's own purposes.
     * Unlike global scope, contents of this map only stay put throughout processing of 
     * current file of augmenting code objects, and then when a new file is encountered,
     * the dictionary is emptied of its contents.
     * @return dictionary scoped to file of augmenting code objects.
     */
    public Map<String, Object> getFileScope() {
        return fileScope;
    }

    /**
     * Gets all of the augmenting code objects for the file currently being processed.
     * @return current file's augmenting code object array.
     */
    public SourceFileAugmentingCode getFileAugCodes() {
        return fileAugCodes;
    }

    void setFileAugCodes(SourceFileAugmentingCode fileAugCodes) {
        this.fileAugCodes = fileAugCodes;
    }

    /**
     * Gets the index of the augmenting code currently being processed.
     * That is, index into the augmenting code object array of the 
     * file currently being processed.
     * @return index of current augmenting code object.
     */
    public int getAugCodeIndex() {
        return augCodeIndex;
    }

    void setAugCodeIndex(int augCodeIndex) {
        this.augCodeIndex = augCodeIndex;
    }

    /**
     * Gets the file of augmenting code objects currently being processed.
     * @return current file of augmenting codes.
     */
    public File getSrcFile() {
        return srcFile;
    }

    void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }
    
    /**
     * Gets a variable from file or global scopes, with 
     * preference for file scope.
     * @param name variable name
     * @return variable value in file scope first, or in global
     * scope.
     */
    public Object getScopeVar(String name) {
        if (fileScope.containsKey(name)) {
            return fileScope.get(name);
        }
        return globalScope.get(name);
    }
    
    /**
     * Sets/Replaces a variable in file scope.
     * @param augCode
     * @param context
     */
    public void setScopeVar(AugmentingCode augCode, ProcessCodeContext context) {
        modifyScope(fileScope, augCode);
    } 
    
    /**
     * Sets/Replaces a variable in global scope.
     * @param augCode
     * @param context
     */
    public void setGlobalScopeVar(AugmentingCode augCode, ProcessCodeContext context) {
        modifyScope(globalScope, augCode);
    }

    @SuppressWarnings("unchecked")
    private static void modifyScope(Map<String, Object> scope, AugmentingCode augCode) {
        for (Object arg : augCode.getArgs()) {
            Map<String, Object> m = (Map<String, Object>) arg;
            for (Map.Entry<String, Object> e: m.entrySet()) {
                String name = e.getKey();
                Object value = e.getValue();
                scope.put(name, value);
            }
        }
    }
}