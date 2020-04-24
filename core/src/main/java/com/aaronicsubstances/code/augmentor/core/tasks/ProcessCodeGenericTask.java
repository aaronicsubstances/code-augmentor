package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
                    augCode.setProcessed(false);
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

                // now process all aug codes.
                int beginErrorCount = allErrors.size();
                for (int i = 0; i < fileAugCodes.getAugmentingCodes().size(); i++) {
                    AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(i);
                    if (augCode.isProcessed()) {
                        continue;
                    }
                    String functionName = augCode.getBlocks().get(0).getContent().trim();
                    context.setAugCodeIndex(i);
                    List<GeneratedCode> genCodes = processAugCode(evalFunction, functionName, 
                        augCode, context, allErrors);
                    fileGenCodes.getGeneratedCodes().addAll(genCodes);
                }

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
    static List<GeneratedCode> processAugCode(EvalFunction evalFunction, 
            String functionName, AugmentingCode augCode, ProcessCodeContext context, 
            List<Throwable> errors) {
        Object result = null;
        try {
            result = evalFunction.apply(functionName, augCode, context);
        }
        catch (Throwable ex) {
            GenericTaskException evalEx = createException(context, ex);
            if (errors == null) {
                throw evalEx;
            }
            errors.add(evalEx);
        }
    
        // Convert various alternative representations of List<GeneratedCode>     
        if (result == null) {
            return Arrays.asList();
        }
        else if (result instanceof Collection) {
            List<GeneratedCode> listResult = new ArrayList<>();
            for (Object listItem : (Collection<Object>)result) {
                GeneratedCode genCode = convertGenCodeItem(listItem);
                listResult.add(genCode);
            }
            return listResult;
        }
        else {
            GeneratedCode genCode = convertGenCodeItem(result);
            genCode.setId(augCode.getId());
            return Arrays.asList(genCode);
        }
    }

    static GeneratedCode convertGenCodeItem(Object item) {
        if (item == null) {
            return null;
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

    private static GenericTaskException createException(ProcessCodeContext context, Throwable cause) {
        int augCodeIndexInContext = context.getAugCodeIndex();
        SourceFileAugmentingCode fileAugCodes = context.getFileAugCodes();
        String srcPath = new File(fileAugCodes.getDir(), fileAugCodes.getRelativePath()).getPath();
        AugmentingCode augCode = fileAugCodes.getAugmentingCodes().get(augCodeIndexInContext);
        String srcFileSnippet = augCode.getBlocks().get(0).getContent();
        String message = srcFileSnippet + ": " + cause;
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