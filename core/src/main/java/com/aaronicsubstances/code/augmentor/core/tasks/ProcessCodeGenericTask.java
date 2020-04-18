package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class ProcessCodeGenericTask {
    // input properties
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private File inputFile;
    private File outputFile;
    private Function<String, Object> jsonParseFunction;
    
    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();

    public void execute(GenericTaskExtensionFunction evalFunction) throws Exception {
        allErrors.clear();
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
                File srcFile = new File(fileAugCodes.getRelativePath());
                TaskUtils.logVerbose(logAppender, "Processing %s", srcFile);
                Instant startInstant = Instant.now();

                // fetch arguments, and parse any json arguments found.
                for (AugmentingCode augCode : fileAugCodes.getAugmentingCodes()) {
                    augCode.setArgs(new ArrayList<>());
                    for (Block block : augCode.getBlocks()) {
                        if (block.isJsonify()) {
                            Object parsedArg = jsonParseFunction.apply(block.getContent());
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
                int beginErrorCount = allErrors.size();
                int i = 0;
                while (i < fileAugCodes.getAugmentingCodes().size()) {
                    AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(i);
                    String functionName = augCode.getBlocks().get(0).getContent().trim();
                    context.put("augCodeIndex", i);
                    List<GeneratedCode> genCodes;
                    try {
                        genCodes = processAugCode(evalFunction, functionName, augCode, context);
                    }
                    catch (GenericTaskException ex) {
                        allErrors.add(ex);
                        i++;
                        continue;
                    }
                    if (genCodes.isEmpty()) {
                        throw new RuntimeException("Should not have empty results here");
                    }
                    fileGenCodes.getGeneratedCodes().addAll(genCodes);
                    i += genCodes.size();
                }

                // now write out generated code for file if no errors are found.
                if (allErrors.size() > beginErrorCount) {
                    TaskUtils.logWarn(logAppender, "%s error(s) encountered in %s. %s", 
                        allErrors.size() - beginErrorCount, fileAugCodes.getFileIndex(), srcFile);
                }
                else if (allErrors.isEmpty()) {
                    fileGenCodes.serialize(responseWriter);
                }

                Instant endInstant = Instant.now();
                long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
                TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", 
                    srcFile, timeElapsed);
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
    List<GeneratedCode> processAugCode(GenericTaskExtensionFunction evalFunction, 
            String functionName, AugmentingCode augCode, Map<String, Object> context) {
        Object result;
        try {
            result = evalFunction.makeFunctionCall(new Object[]{ functionName, augCode, context });
        }
        catch (Throwable ex) {
            throw createException(context, ex);
        }
    
        // validate return result: must be list of objects, single object or string.
        if (result instanceof List) {
            List<GeneratedCode> listResult = (List<GeneratedCode>) result;
            if (listResult.isEmpty()) {
                throw createException(context, "Received empty results");
            }
            int augCodeIndexInContext = (int) context.get("augCodeIndex");
            SourceFileAugmentingCode fileAugCodes = (SourceFileAugmentingCode) context.get("fileAugCodes");
            for (int j = 0; j < listResult.size(); j++) {
                GeneratedCode genCode = listResult.get(j);
                // instead of throwing error, rather save them here so
                // we can skip past whatever aug codes are targeted, and
                // likely avoid superflous errors due to intermediate/dependent aug codes.
                if (genCode.getContent() == null) {
                    allErrors.add(createException(context, "content property is not set"));
                    break;
                }
                if (augCodeIndexInContext + j >= fileAugCodes.getAugmentingCodes().size()) {
                    allErrors.add(createException(context, "No aug code found at offset " + j));
                    break;
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
                if (genCode.getContent() == null) {
                    throw createException(context, "content property is not set");
                }
                genCode.setIndex(augCode.getIndex());
                return Arrays.asList(genCode);
            }
            else if (result == null) {
                throw createException(context, "Received null");
            }
            else {
                throw createException(context, "Received unexpected result type: " + result.getClass());
            }
        }
    }

    private static GenericTaskException createException(Map<String, Object> context, Object excOrMsg) {
        int augCodeIndexInContext = (int) context.get("augCodeIndex");
        SourceFileAugmentingCode fileAugCodes = (SourceFileAugmentingCode) context.get("fileAugCodes");
        String srcPath = fileAugCodes.getRelativePath();
        AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(augCodeIndexInContext);
        String srcFileSnippet = augCode.getBlocks().get(0).getContent();
        String message = srcFileSnippet + ": ";
        Throwable cause;
        if (excOrMsg instanceof String) {
            message += (String) excOrMsg;
            cause = null;
        }
        else {
            cause = (Throwable) excOrMsg;
            message += cause;
        }
        return GenericTaskException.create(cause, message, srcPath, augCode.getLineNumber(), null);
    }

    public BiConsumer<GenericTaskLogLevel, Supplier<String>> getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender) {
        this.logAppender = logAppender;
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public Function<String, Object> getJsonParseFunction() {
        return jsonParseFunction;
    }

    public void setJsonParseFunction(Function<String, Object> jsonParseFunction) {
        this.jsonParseFunction = jsonParseFunction;
    }

    public List<Throwable> getAllErrors() {
        return allErrors;
    }
}