package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
        Object apply(String function, AugmentingCode augCode, ProcessCodeContext context) throws Exception;
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
            StringBuilder headerLine = new StringBuilder();
            requestReader = codeGenRequest.beginDeserialize(inputFile, headerLine);
            // populate global scope with file header line
            Object parsedHeaderLine = jsonParseFunction.parse(headerLine.toString());
            context.setHeader(parsedHeaderLine);

            responseWriter = codeGenResponse.beginSerialize(outputFile);
            SourceFileAugmentingCode fileAugCodes;
            while ((fileAugCodes = SourceFileAugmentingCode.deserialize(requestReader)) != null) {
                // set up context.
                context.setSrcFile(new File(fileAugCodes.getDir(), fileAugCodes.getRelativePath()));
                context.setFileAugCodes(fileAugCodes);
                context.getFileScope().clear();

                TaskUtils.logVerbose(logAppender, "Processing %s", context.getSrcFile());
                Instant startInstant = Instant.now();

                // fetch arguments, and parse any json arguments found.
                for (AugmentingCode augCode : fileAugCodes.getAugmentingCodes()) {
                    augCode.setProcessed(false);
                    augCode.setArgs(new ArrayList<>());
                    for (Block block : augCode.getBlocks()) {
                        if (block.isJsonify()) {
                            Object parsedArg = jsonParseFunction.parse(block.getContent());
                            augCode.getArgs().add(parsedArg);
                        } else if (block.isStringify()) {
                            augCode.getArgs().add(block.getContent());
                        }
                    }
                }

                SourceFileGeneratedCode fileGenCodes = new SourceFileGeneratedCode(new ArrayList<>());
                fileGenCodes.setFileId(fileAugCodes.getFileId());

                // now process all aug codes.
                int beginErrorCount = allErrors.size();
                for (int i = 0; i < fileAugCodes.getAugmentingCodes().size(); i++) {
                    AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(i);
                    if (augCode.isProcessed()) {
                        continue;
                    }
                    String functionName = augCode.getBlocks().get(0).getContent().trim();
                    context.setAugCodeIndex(i);
                    List<GeneratedCode> genCodes = processAugCode(evalFunction, functionName, augCode, context,
                            allErrors);
                    fileGenCodes.getGeneratedCodes().addAll(genCodes);
                }

                validateGeneratedCodeIds(fileGenCodes.getGeneratedCodes(), context, allErrors);

                // now write out generated code for file if no errors are found.
                if (allErrors.size() > beginErrorCount) {
                    TaskUtils.logWarn(logAppender, "%s error(s) encountered in %s", allErrors.size() - beginErrorCount,
                            context.getSrcFile());
                }

                // don't waste time serializing if there are errors from previous
                // iterations or this current one.
                if (allErrors.isEmpty()) {
                    fileGenCodes.serialize(responseWriter);
                }

                Instant endInstant = Instant.now();
                long timeElapsed = Duration.between(startInstant, endInstant).toMillis();
                TaskUtils.logInfo(logAppender, "Done processing %s in %d ms", context.getSrcFile(), timeElapsed);
            }
        } finally {
            if (requestReader != null) {
                codeGenRequest.endDeserialize(requestReader);
            }
            if (responseWriter != null) {
                codeGenResponse.endSerialize(responseWriter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static List<GeneratedCode> processAugCode(EvalFunction evalFunction, 
            String functionName, AugmentingCode augCode, ProcessCodeContext context, 
            List<Throwable> errors) {
        try {
            Object result = evalFunction.apply(functionName, augCode, context);
    
            // Convert various alternative representations of List<GeneratedCode>     
            if (result == null) {
                return Arrays.asList();
            }
            else if (result instanceof Collection) {
                List<GeneratedCode> listResult = new ArrayList<>();
                for (Object listItem : (Collection<Object>)result) {
                    GeneratedCode genCode = convertGenCodeItem(listItem);
                    listResult.add(genCode);
                    // try and mark corresponding aug code as processed.
                    if (genCode.getId() > 0) {
                        Optional<AugmentingCode> correspondingAugCode = 
                            context.getFileAugCodes().getAugmentingCodes().stream()
                                .filter(x -> x.getId() == genCode.getId())
                                .findFirst();
                        if (correspondingAugCode.isPresent()) {
                            correspondingAugCode.get().setProcessed(true);
                        }
                    }
                }
                return listResult;
            }
            else {
                GeneratedCode genCode = convertGenCodeItem(result);
                genCode.setId(augCode.getId());
                return Arrays.asList(genCode);
            }
        }
        catch (Throwable ex) {
            GenericTaskException evalEx = createException(context, null, ex);
            errors.add(evalEx);
            return Arrays.asList();
        }
    }

    static GeneratedCode convertGenCodeItem(Object item) {
        if (item == null) {
            return new GeneratedCode();
        }
        else if (item instanceof GeneratedCode) {
            GeneratedCode genCode = (GeneratedCode) item;
            return genCode;
        }
        else if (item instanceof ContentPart) {
            GeneratedCode genCode = new GeneratedCode(Arrays.asList((ContentPart) item));
            return genCode;
        }
        else {
            // stringify by default any non-null object.
            GeneratedCode genCode = new GeneratedCode(new ArrayList<>());
            genCode.getContentParts().add(new ContentPart(item.toString(), false));
            return genCode;
        }
    }

    static void validateGeneratedCodeIds(List<GeneratedCode> genCodes,
            ProcessCodeContext context, List<Throwable> errors) {
        List<Integer> ids = genCodes.stream().map(x -> x.getId())
            .collect(Collectors.toList());
        // Interpret use of -1 or negatives as intentional and skip
        // validating negative ids.
        if (ids.stream().anyMatch(x -> x == 0)) {
            errors.add(createException(context,
                "At least one generated code id was not set. Found: " +
                    ids, null));
        }
        else if (ids.stream().filter(x -> x > 0).distinct().count() < ids.size()) {
            errors.add(createException(context,
                "Valid generated code ids must be unique, but found duplicates: " + ids, null));
        }
    }

    private static GenericTaskException createException(ProcessCodeContext context, String message,
            Throwable evalEx) {
        SourceFileAugmentingCode fileAugCodes = context.getFileAugCodes();
        int lineNumber = -1;
        String fullMessage = message;
        if (evalEx != null) {
            int augCodeIndexInContext = context.getAugCodeIndex();
            AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(augCodeIndexInContext);
            lineNumber = augCode.getLineNumber();
            String srcFileSnippet = augCode.getBlocks().get(0).getContent();
            fullMessage = srcFileSnippet + ": " + evalEx;
        }
        return GenericTaskException.create(evalEx, fullMessage, context.getSrcFile().getPath(), 
            lineNumber, null);
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