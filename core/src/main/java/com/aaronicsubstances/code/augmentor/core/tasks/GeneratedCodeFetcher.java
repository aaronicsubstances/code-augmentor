package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;

public class GeneratedCodeFetcher {
    private final List<CodeGenerationResponse> codeGenerationResponses;
    private final List<Object> codeGenerationResponseReaders;
    private final List<SourceFileGeneratedCode> lastFetches;

    public GeneratedCodeFetcher(List<File> generatedCodeFiles) throws Exception {
        this(generatedCodeFiles.toArray());
    }

    /**
     * For testing purposes.
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

    public void close() throws Exception {        
        for (int i = 0; i < codeGenerationResponses.size(); i++) {
            CodeGenerationResponse instance = codeGenerationResponses.get(i);
            Object codeGenRespRdr = codeGenerationResponseReaders.get(i);
            instance.endDeserialize(codeGenRespRdr);
        }
    }
    
    public void prepareForFile(int fileIndex) throws Exception {
        boolean firstPrepare = false;
        if (lastFetches.isEmpty()) {
            firstPrepare = true;
            for (int i = 0; i < codeGenerationResponses.size(); i++) {
                lastFetches.add(null);
            }
        }
        for (int i = 0; i < lastFetches.size(); i++) {
            SourceFileGeneratedCode fileGenCode;
            if (!firstPrepare) {
                fileGenCode = lastFetches.get(i);
                if (fileGenCode == null || fileIndex <= fileGenCode.getFileIndex()) {
                    continue;
                }
            }
            Object rdr = codeGenerationResponseReaders.get(i);
            fileGenCode = SourceFileGeneratedCode.deserialize(rdr);
            lastFetches.set(i, fileGenCode);
        }
    }

    public GeneratedCode getGeneratedCode(int fileIndex, int augCodeIndex, 
            StringBuilder newlineReceiver) throws Exception {
        GeneratedCode nextGenCode = null;
        for (int i = 0; i < lastFetches.size(); i++) {
            CodeGenerationResponse codeGenRes = codeGenerationResponses.get(i);
            SourceFileGeneratedCode fileGenCode = lastFetches.get(i);
            if (fileGenCode != null && fileGenCode.getFileIndex() == fileIndex) {
                Optional<GeneratedCode> genCodeOpt = fileGenCode.getGeneratedCodeList()
                    .stream().filter(x -> x.getIndex() == augCodeIndex).findFirst();
                    
                if (genCodeOpt.isPresent()) {
                    nextGenCode = genCodeOpt.get();
                    if (fileGenCode.getNewline() != null) {
                        newlineReceiver.append(fileGenCode.getNewline());
                    }
                    else if (codeGenRes.getNewline() != null) {
                        newlineReceiver.append(codeGenRes.getNewline());
                    }
                    else {
                        newlineReceiver.setLength(0);
                    }
                    break;
                }
            }
        }
        return nextGenCode;
    }
}