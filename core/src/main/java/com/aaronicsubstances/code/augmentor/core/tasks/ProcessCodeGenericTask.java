package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationRequest;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileAugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.SourceFileGeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;

public class ProcessCodeGenericTask {

    public interface JsonParseFunction {
        Object parse(String json) throws Exception;
    }

    public interface EvalFunction {
        Object apply(String function, AugmentingCode augCode, ProcessCodeContext context);
    }

    // input properties
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private File inputFile;
    private File outputFile;
    private JsonParseFunction jsonParseFunction;
    
    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();

    public void execute(EvalFunction evalFunction) throws Exception {
        allErrors.clear();
        // ensure dir exists for outputFile
        outputFile.getParentFile().mkdirs();

        // create a context for sharing variables needed when
        // an aug code section is used more than once in a file, or
        // even throughout file sets.
        ProcessCodeContext context = new ProcessCodeContext();

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
                            Object parsedArg = jsonParseFunction.parse(block.getContent());
                            augCode.getArgs().add(parsedArg);
                        }
                        else if (block.isStringify()) {
                            augCode.getArgs().add(block.getContent());
                        }
                    }
                }

                // set up context.
                context.setFileAugCodes(fileAugCodes);
                context.getFileScope().clear();

                SourceFileGeneratedCode fileGenCodes = new SourceFileGeneratedCode(new ArrayList<>());
                fileGenCodes.setFileId(fileAugCodes.getFileId());

                // Deterministic looping already ensures that once an aug code
                // is processed, it won't be processed again.
                // forwardProcessedAugCodeIndices is vital only for multiple aug
                // code results, in which all but the current one have to be
                // stored to prevent another pass of processing on them.
                List<Integer> forwardProcessedAugCodeIds = new ArrayList<>();

                // now process all aug codes.
                int beginErrorCount = allErrors.size();
                for (int i = 0; i < fileAugCodes.getAugmentingCodes().size(); i++) {
                    AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(i);
                    if (forwardProcessedAugCodeIds.contains(augCode.getId())) {
                        continue;
                    }
                    String functionName = augCode.getBlocks().get(0).getContent().trim();
                    context.setAugCodeIndex(i);
                    try {
                        List<GeneratedCode> genCodes = processAugCode(evalFunction, functionName, 
                            augCode, context, forwardProcessedAugCodeIds);
                        fileGenCodes.getGeneratedCodes().addAll(genCodes);
                    }
                    catch (GenericTaskException ex) {
                        allErrors.add(ex);
                    }
                }

                // Sort by id
                fileGenCodes.getGeneratedCodes().sort((g1, g2) -> 
                    Integer.compare(g1.getId(), g2.getId()));

                // now write out generated code for file if no errors are found.
                if (allErrors.size() > beginErrorCount) {
                    TaskUtils.logWarn(logAppender, "%s error(s) encountered in %s", 
                        allErrors.size() - beginErrorCount, srcFile);
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
    List<GeneratedCode> processAugCode(EvalFunction evalFunction, 
            String functionName, AugmentingCode augCode, ProcessCodeContext context,
            List<Integer> forwardProcessedAugCodeIds) {
        Object result;
        try {
            result = evalFunction.apply(functionName, augCode, context);
        }
        catch (Throwable ex) {
            throw createException(context, ex);
        }
    
        // Validate eval function return result.
        // As much as possible, record errors rather than throw them so
        // we can skip past whatever aug codes are targeted, and
        // likely avoid superflous errors due to intermediate/dependent aug codes.     
        if (result == null) {
            throw createException(context, "Received null result");
        }
        else if (result instanceof List) {
            List<GeneratedCode> listResult = (List<GeneratedCode>) result;
            if (listResult.isEmpty()) {
                throw createException(context, "Received empty list");
            }
            int augCodeIndexInContext = context.getAugCodeIndex();
            SourceFileAugmentingCode fileAugCodes = context.getFileAugCodes();
            validateAugCodeIndices(augCode.getId(), listResult, context);
            for (int j = 0; j < listResult.size(); j++) {
                GeneratedCode genCode = listResult.get(j);
                if (genCode == null) {
                    allErrors.add(createException(context, "Found null list item at index " + j));
                    continue;
                }
                if (genCode.getId() <= 0) {
                    if (augCodeIndexInContext + j >= fileAugCodes.getAugmentingCodes().size()) {
                        allErrors.add(createException(context, "No aug code found at offset " + j));
                        continue;
                    }
                    AugmentingCode correspondingAugCode = fileAugCodes.getAugmentingCodes().get(
                        augCodeIndexInContext + j);
                    genCode.setId(correspondingAugCode.getId());
                }
                else {
                    if (genCode.getId() < augCode.getId() || 
                            forwardProcessedAugCodeIds.contains(genCode.getId())) {
                        allErrors.add(createException(context, "Aug code with id " + genCode.getId() +
                            " has already been processed."));
                        continue;
                    }
                    Optional<AugmentingCode> correspondingAugCode = fileAugCodes.getAugmentingCodes().stream().
                        filter(x -> x.getId() == genCode.getId()).findAny();
                    if (!correspondingAugCode.isPresent()) {                        
                        allErrors.add(createException(context, "Aug code with id " + genCode.getId() +
                            " was not found."));
                        continue;
                    }
                    genCode.setId(correspondingAugCode.get().getId());
                }
                validateContentParts(genCode, j, context);
                forwardProcessedAugCodeIds.add(genCode.getId());
            }
            return listResult;
        }
        else if (result instanceof GeneratedCode) {
            GeneratedCode genCode = (GeneratedCode) result;
            genCode.setId(augCode.getId());
            validateContentParts(genCode, -1, context);
            return Arrays.asList(genCode);
        }
        else if (result instanceof ContentPart) {
            GeneratedCode genCode = new GeneratedCode(Arrays.asList((ContentPart) result));
            genCode.setId(augCode.getId());
            validateContentParts(genCode, -1, context);
            return Arrays.asList(genCode);
        }
        else {
            GeneratedCode genCode = new GeneratedCode(new ArrayList<>());
            genCode.setId(augCode.getId());
            genCode.getContentParts().add(new ContentPart(result.toString(), false));
            validateContentParts(genCode, -1, context);
            return Arrays.asList(genCode);
        }
    }

    private void validateAugCodeIndices(int currentAugCodeId, List<GeneratedCode> listResult,
            ProcessCodeContext context) {
        List<Integer> ids = listResult.stream().map(x -> x.getId())
            .collect(Collectors.toList());
        // For aug code indices to be valid,
        // either they are not set, ie they are all not positive,
        if (ids.stream().allMatch(x -> x <= 0)) {
            return;
        }
        // OR, they meet all of the ff conditions:
        //   1. they have all been set, ie they are all positive.
        //   2. they are distinct. can be out of order, but musts be distinct.
        //   3. they must include current aug code id.
        if (!ids.stream().allMatch(x -> x > 0)) {
            throw createException(context, "Not all aug code ids are set: " + ids +
                ". If any aug code id is set, then all must be set as well.");
        }
        if (ids.stream().distinct().count() != listResult.size()) {
            throw createException(context, "Duplicates detected among provided aug code ids: " +
                ids);
        }
        if (!ids.contains(currentAugCodeId)) {
            throw createException(context, 
                "Current aug code id " + currentAugCodeId +
                " not found amoung provided ids: " + ids +
                ". If all aug code ids are set, then current aug code id must be set as well.");
        }
    }

    private void validateContentParts(GeneratedCode genCode, int genCodeIndex,
            ProcessCodeContext context) {
        String errorSuffix = "";
        if (genCodeIndex != -1) {
            errorSuffix = "in list item at index " + genCodeIndex;
        }
        List<ContentPart> parts = genCode.getContentParts();
        if (parts == null || parts.isEmpty()) {
            allErrors.add(createException(context, "Found null/empty content parts" + errorSuffix));
        }
        for (int i = 0; i < parts.size(); i++) {
            ContentPart part = parts.get(i);
            if (part == null || part.getContent() == null) {
                allErrors.add(createException(context, "Found null part/content at index " + i +
                    errorSuffix));
            }
        }
    }

    private static GenericTaskException createException(ProcessCodeContext context, Object excOrMsg) {
        int augCodeIndexInContext = context.getAugCodeIndex();
        SourceFileAugmentingCode fileAugCodes = context.getFileAugCodes();
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

    public JsonParseFunction getJsonParseFunction() {
        return jsonParseFunction;
    }

    public void setJsonParseFunction(JsonParseFunction jsonParseFunction) {
        this.jsonParseFunction = jsonParseFunction;
    }

    public List<Throwable> getAllErrors() {
        return allErrors;
    }
}