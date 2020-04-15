package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;

public abstract class ProcessCodeGenericTask {

    public void execute(File inputFile, File outputFile) throws Exception {
        // ensure dir exists for outputFile
        outputFile.getParentFile().mkdirs();

        // create a context for sharing variables needed when
        // an aug code section is used more than once in a file, or
        // even throughout file sets.
        Map<String, Object> context = new HashMap<>();
        context.put("globalScope", new HashMap<>());
        // provide convenience way to create GeneratedCode instances.
        context.put("genCodeSupplier", new Supplier<GeneratedCode>() {
            @Override
            public GeneratedCode get() {
                return new GeneratedCode();
            }
        });

        CodeGenerationRequest codeGenRequest = new CodeGenerationRequest();
        CodeGenerationResponse codeGenResponse = new CodeGenerationResponse();
        Object requestReader = null, responseWriter = null;
        try {
            requestReader = codeGenRequest.beginDeserialize(inputFile);
            responseWriter = codeGenResponse.beginSerialize(outputFile);
            SourceFileAugmentingCode fileAugCodes;
            while ((fileAugCodes = SourceFileAugmentingCode.deserialize(requestReader)) != null) {
                // fetch arguments, and parse any json arguments found.
                for (AugmentingCode augCode : fileAugCodes.getAugmentingCodes()) {
                    augCode.setArgs(new ArrayList<>());
                    for (Block block : augCode.getBlocks()) {
                        if (block.isJsonify()) {
                            Object parsedArg = parseJsonArg(block.getContent());
                            augCode.getArgs().add(parsedArg);
                        }
                        else if (block.isStringify()) {
                            augCode.getArgs().add(block.getContent());
                        }
                    }
                }

                // set up context.
                context.put("fileAugCodes", fileAugCodes);
                context.put("fileScope", new HashMap<>());

                SourceFileGeneratedCode fileGenCodes = new SourceFileGeneratedCode(new ArrayList<>());
                fileGenCodes.setFileIndex(fileAugCodes.getFileIndex());

                // now process all aug codes.
                int i = 0;
                while (i < fileAugCodes.getAugmentingCodes().size()) {
                    AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(i);
                    String functionName = augCode.getBlocks().get(0).getContent().trim();
                    context.put("augCodeIndex", i);
                    List<GeneratedCode> genCodes = processAugCode(functionName, augCode, context);
                    if (genCodes.isEmpty()) {
                        throw new RuntimeException("Should not have empty results here");
                    }
                    fileGenCodes.getGeneratedCodes().addAll(genCodes);
                    i += genCodes.size();
                }

                // now write out generated code for file.
                fileGenCodes.serialize(responseWriter);
            }
        }
        finally {
            if (requestReader != null) {
                codeGenRequest.endDeserialize(requestReader);
            }
            if (responseWriter != null) {
                codeGenResponse.endSerialize(responseWriter);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    List<GeneratedCode> processAugCode(String functionName, AugmentingCode augCode, 
            Map<String, Object> context) {
        int augCodeIndexInContext = (int) context.get("augCodeIndex");
        Object result;
        try {
            result = callUserFunction(functionName, augCode, context);
        }
        catch (Exception ex) {
            return createErrorGenCode(augCodeIndexInContext, ex);
        }
    
        // validate return result: must be list of objects, single object or string.
        if (result instanceof List) {
            List<GeneratedCode> listResult = (List<GeneratedCode>) result;
            if (listResult.isEmpty()) {
                return createErrorGenCode(augCodeIndexInContext, "Received empty results");
            }
            SourceFileAugmentingCode fileAugCodes = (SourceFileAugmentingCode) context.get("fileAugCodes");
            for (int j = 0; j < listResult.size(); j++) {
                GeneratedCode genCode = listResult.get(j);
                if (augCodeIndexInContext + j >= fileAugCodes.getAugmentingCodes().size()) {
                    return createErrorGenCode(augCodeIndexInContext, "No aug code found at offset " + j);
                }
                AugmentingCode correspondingAugCode = fileAugCodes.getAugmentingCodes().get(
                    augCodeIndexInContext + j);
                genCode.setIndex(correspondingAugCode.getIndex());
            }
            return listResult;
        }
        else if (result instanceof String) {
            GeneratedCode genCode = new GeneratedCode();
            genCode.setContent((String) result);
            genCode.setIndex(augCode.getIndex());
            return Arrays.asList(genCode);
        }
        else {
            // must be object or else error.            
            if (result instanceof GeneratedCode) {
                GeneratedCode genCode = (GeneratedCode) result;
                genCode.setIndex(augCode.getIndex());
                return Arrays.asList(genCode);
            }
            else if (result == null) {
                return createErrorGenCode(augCodeIndexInContext, "Received null");
            }
            else {
                return createErrorGenCode(augCodeIndexInContext, "Received unexpected result type: " + result.getClass());
            }
        }
    }

    private static List<GeneratedCode> createErrorGenCode(int augCodeIndex, Object errOrMsg) {
        GeneratedCode errorGenCode = new GeneratedCode();
        errorGenCode.setIndex(augCodeIndex);
        errorGenCode.setError(true);
        errorGenCode.setContent(errOrMsg.toString());
        return Arrays.asList(errorGenCode);
    }
    
    protected abstract Object parseJsonArg(String str);
    protected abstract Object callUserFunction(String functionName, AugmentingCode augCode,
        Map<String, Object> context);
}