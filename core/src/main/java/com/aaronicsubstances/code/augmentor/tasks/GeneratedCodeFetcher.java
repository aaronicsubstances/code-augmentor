package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aaronicsubstances.code.augmentor.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.models.SourceFileGeneratedCode;

public class GeneratedCodeFetcher {
    private final List<Object> codeGenerationResponseReaders;
    private final List<SourceFileGeneratedCode> lastFetches;

    public GeneratedCodeFetcher(List<File> generatedCodeFiles) throws Exception {
        this.codeGenerationResponseReaders = new ArrayList<>();
        for (File f : generatedCodeFiles) {
            Object codeGenRespRdr = CodeGenerationResponse.beginDeserialize(f);
            codeGenerationResponseReaders.add(codeGenRespRdr);
        }
        lastFetches = new ArrayList<>();
    }

	public void close() throws Exception {        
        for (Object codeGenRespRdr : codeGenerationResponseReaders) {
            CodeGenerationResponse.endDeserialize(codeGenRespRdr);
        }
    }
    
    public void prepareForFile(int fileIndex) throws Exception {
        boolean firstPrepare = false;
        if (lastFetches.isEmpty()) {
            firstPrepare = true;
            for (int i = 0; i < codeGenerationResponseReaders.size(); i++) {
                lastFetches.add(null);
            }
        }
        for (int i = 0; i < lastFetches.size(); i++) {
            SourceFileGeneratedCode fileGenCode;
            if (!firstPrepare) {
                fileGenCode = lastFetches.get(i);
                if (fileGenCode == null || fileGenCode.getFileIndex() != fileIndex) {
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
            SourceFileGeneratedCode fileGenCode = lastFetches.get(i);
            if (fileGenCode == null || fileGenCode.getFileIndex() != fileIndex) {
                continue;
            }
            Optional<GeneratedCode> genCodeOpt = fileGenCode.getGeneratedCodeList()
                .stream().filter(x -> x.getIndex() == augCodeIndex).findFirst();
            if (genCodeOpt.isPresent()) {
                nextGenCode = genCodeOpt.get();
                break;
            }
        }
        return nextGenCode;
	}
}