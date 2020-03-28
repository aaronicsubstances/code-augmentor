package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aaronicsubstances.code.augmentor.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.models.SourceFileGeneratedCode;

public class GeneratedCodeFetcher {
    private final List<CodeGenerationResponse> codeGenerationResponses;
    private final List<Object> codeGenerationResponseReaders;
    private List<SourceFileGeneratedCode> lastFetches;

    public GeneratedCodeFetcher(List<File> generatedCodeFiles) throws Exception {
        this.codeGenerationResponses = new ArrayList<>();
        this.codeGenerationResponseReaders = new ArrayList<>();
        lastFetches = new ArrayList<>();
        for (File f : generatedCodeFiles) {
            CodeGenerationResponse codeGenResp = new CodeGenerationResponse();
            codeGenerationResponses.add(codeGenResp);
            boolean useXml = TaskUtils.canUseXml(f);
            Object codeGenRespRdr = codeGenResp.beginDeserializer(f, useXml);
            codeGenerationResponseReaders.add(codeGenRespRdr);
        }
    }

	public void close() throws Exception {        
        for (int i = 0; i < codeGenerationResponses.size(); i++) {
            CodeGenerationResponse codeGenResp = codeGenerationResponses.get(i);
            Object codeGenRespRdr = codeGenerationResponseReaders.get(i);
            codeGenResp.endDeserialize(codeGenRespRdr);
        }
    }
    
    public void prepareForFile(int fileIndex) throws Exception {
        boolean firstPrepare = false;
        if (lastFetches == null) {
            lastFetches = new ArrayList<>();
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
            fileGenCode = new SourceFileGeneratedCode();
            Object rdr = codeGenerationResponseReaders.get(i);
            if (!fileGenCode.deserialize(rdr)) {
                fileGenCode = null;
            }
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