package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
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
        this.codeGenerationResponses = new ArrayList<>();
        this.codeGenerationResponseReaders = new ArrayList<>();
        for (File f : generatedCodeFiles) {
            CodeGenerationResponse instance = new CodeGenerationResponse();
            codeGenerationResponses.add(instance);
            Object codeGenRespRdr = instance.beginDeserialize(f);
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

	public GeneratedCode getGeneratedCode(int fileIndex, int augCodeIndex) throws Exception {
        GeneratedCode nextGenCode = null;
		for (int i = 0; i < lastFetches.size(); i++) {
            Optional<GeneratedCode> genCodeOpt = null;
            // check whether codeGenRes already has all generated codes loaded.
            // used during testing, and in theory can be used when it is
            // deemed not to be a problem for memory.
            CodeGenerationResponse codeGenRes = codeGenerationResponses.get(i);
            if (!codeGenRes.getSourceFileGeneratedCodeList().isEmpty()) {
                genCodeOpt = codeGenRes.getSourceFileGeneratedCodeList()
                    .stream().filter(x -> x.getFileIndex() == fileIndex)
                    .flatMap(x -> x.getGeneratedCodeList().stream())
                    .filter(x -> x.getIndex() == augCodeIndex).findFirst();
            }
            else {
                SourceFileGeneratedCode fileGenCode = lastFetches.get(i);
                if (fileGenCode != null && fileGenCode.getFileIndex() == fileIndex) {
                    genCodeOpt = fileGenCode.getGeneratedCodeList()
                        .stream().filter(x -> x.getIndex() == augCodeIndex).findFirst();
                }
            }
            if (genCodeOpt != null && genCodeOpt.isPresent()) {
                nextGenCode = genCodeOpt.get();
                break;
            }
        }
        /*if (nextGenCode == null) {
            System.out.println("codeGenerationResponses for (" + fileIndex + ", " + augCodeIndex + "): " + 
                codeGenerationResponses);
            System.out.println("lastFetches: " + lastFetches);
        }*/
        return nextGenCode;
	}
}