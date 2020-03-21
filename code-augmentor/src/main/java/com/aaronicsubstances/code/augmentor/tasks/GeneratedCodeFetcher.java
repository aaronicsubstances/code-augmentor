package com.aaronicsubstances.code.augmentor.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.models.GeneratedCode;

/**
 * GeneratedCodeFetcher
 */
public class GeneratedCodeFetcher {
    private final List<CodeGenerationResponse> codeGenerationResponses;
    private final List<Object> codeGenerationResponseReaders;
    private final List<GeneratedCode> lastFetches;

    public GeneratedCodeFetcher(List<File> generatedCodeFiles) throws Exception {
        this.codeGenerationResponses = new ArrayList<>();
        this.codeGenerationResponseReaders = new ArrayList<>();
        for (File f : generatedCodeFiles) {
            CodeGenerationResponse codeGenResp = new CodeGenerationResponse();
            codeGenerationResponses.add(codeGenResp);
            boolean useXml = !"csv".equals(TaskUtils.getFileExt(f.getName()));
            Object codeGenRespRdr = codeGenResp.beginDeserializer(f, useXml);
            codeGenerationResponseReaders.add(codeGenRespRdr);
        }

        // prime fetcher.
        lastFetches = new ArrayList<>();
        for (Object rdr : codeGenerationResponseReaders) {
            GeneratedCode genCode = new GeneratedCode();
            if (!genCode.deserialize(rdr)) {
                genCode = null;
            }
            lastFetches.add(genCode);
        }
    }

	public void close() throws Exception {        
        for (int i = 0; i < codeGenerationResponses.size(); i++) {
            CodeGenerationResponse codeGenResp = codeGenerationResponses.get(i);
            Object codeGenRespRdr = codeGenerationResponseReaders.get(i);
            codeGenResp.endDeserialize(codeGenRespRdr);
        }
	}

	public GeneratedCode getGeneratedCode(int augCodeIndex) throws Exception {
        GeneratedCode nextGenCode = null;
		for (int i = 0; i < lastFetches.size(); i++) {
            GeneratedCode genCode = lastFetches.get(i);
            if (genCode == null || genCode.getIndex() != augCodeIndex) {
                continue;
            }
            nextGenCode = genCode;
            // move to next gen code at index i.
            Object rdr = codeGenerationResponseReaders.get(i);
            genCode = new GeneratedCode();
            if (!genCode.deserialize(rdr)) {
                genCode = null;
            }
            lastFetches.set(i, genCode);
        }
        return nextGenCode;
	}
}