package com.aaronicsubstances.code.augmentor.core.util;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;

/**
 * This class is responsible for matching generated code objects to their
 * descriptors.
 */
public class GeneratedCodeFetcher {
    private final List<CodeGenerationResponse> codeGenerationResponses;
    private final List<Object> codeGenerationResponseReaders;
    private final List<SourceFileGeneratedCode> lastFetches;

    /**
     * Constructor for normal/production usage of class with
     * code generation response files.
     */
    public GeneratedCodeFetcher(List<File> generatedCodeFiles) throws Exception {
        this(generatedCodeFiles.toArray());
    }

    /**
     * Constructor which enables testing.
     * @param codeGenerationResponseSources array of files or strings.
     * @throws Exception
     */
    GeneratedCodeFetcher(Object[] codeGenerationResponseSources) throws Exception {
        this.codeGenerationResponses = new ArrayList<>();
        this.codeGenerationResponseReaders = new ArrayList<>();
        for (Object codeGenerationResponseSource : codeGenerationResponseSources) {
            CodeGenerationResponse instance = new CodeGenerationResponse();
            codeGenerationResponses.add(instance);
            Object codeGenRespRdr;
            if (codeGenerationResponseSource instanceof File) {
                codeGenRespRdr = instance.beginDeserialize((File) codeGenerationResponseSource);
            }
            else {
                // must be string for testing purposes.
                String serializedSource = (String) codeGenerationResponseSource;
                codeGenRespRdr = instance.beginDeserialize(new StringReader(serializedSource));
            }
            codeGenerationResponseReaders.add(codeGenRespRdr);
        }
        lastFetches = new ArrayList<>();
    }

    /**
     * Closes all opened code generation response files.
     * @throws Exception
     */
    public void close() throws Exception {        
        for (int i = 0; i < codeGenerationResponses.size(); i++) {
            CodeGenerationResponse instance = codeGenerationResponses.get(i);
            Object codeGenRespRdr = codeGenerationResponseReaders.get(i);
            instance.endDeserialize(codeGenRespRdr);
        }
    }
    
    /**
     * Advances internal pointer of this object to locate a file of
     * generated code objects. Must be called before attempts are
     * made to find generated code objects for a file.
     * @param fileId id of file of generated code objects.
     * @return true if fileId was found; false if it was not found.
     * @throws Exception
     */
    public boolean prepareForFile(int fileId) throws Exception {
        boolean firstPrepare = false;
        if (lastFetches.isEmpty()) {
            firstPrepare = true;
            for (int i = 0; i < codeGenerationResponses.size(); i++) {
                lastFetches.add(null);
            }
        }
        boolean found = false;
        for (int i = 0; i < lastFetches.size(); i++) {
            SourceFileGeneratedCode fileGenCode;
            if (!firstPrepare) {
                fileGenCode = lastFetches.get(i);
                if (fileGenCode == null) {
                    // meaning no more files
                    continue;
                }
                else if (fileId == fileGenCode.getFileId()) {
                    found = true;
                    continue;
                }
                else {
                    // this part assumes file ids are in ascending order.
                    // So if peeking reveals file with id higher than fileId,
                    // then it means we are not going to find fileId.
                    if (fileId < fileGenCode.getFileId()) {                        
                        continue;
                    }
                }
            }
            Object rdr = codeGenerationResponseReaders.get(i);
            fileGenCode = SourceFileGeneratedCode.deserialize(rdr);
            lastFetches.set(i, fileGenCode);
            if (fileGenCode != null && fileId == fileGenCode.getFileId()) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Finds a generated code in a file.
     * @param fileId id of file
     * @param augCodeId id of corresponding aug code
     * @return generated code object or null if not found.
     * @throws Exception
     */
    public GeneratedCode getGeneratedCode(int fileId, int augCodeId) throws Exception {
        GeneratedCode nextGenCode = null;
        for (int i = 0; i < lastFetches.size(); i++) {
            SourceFileGeneratedCode fileGenCode = lastFetches.get(i);
            if (fileGenCode != null && fileGenCode.getFileId() == fileId) {
                Optional<GeneratedCode> genCodeOpt = fileGenCode.getGeneratedCodes()
                    .stream().filter(x -> x.getId() == augCodeId).findFirst();
                    
                if (genCodeOpt.isPresent()) {
                    nextGenCode = genCodeOpt.get();
                    break;
                }
            }
        }
        return nextGenCode;
    }
}