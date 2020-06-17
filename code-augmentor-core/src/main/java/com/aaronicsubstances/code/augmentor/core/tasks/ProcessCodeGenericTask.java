package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

/**
 * Intended for use by plugins and scripts written in Groovy (and other JVM languages)
 * to implement processing stage of Code Augmentor.
 */
public class ProcessCodeGenericTask {

    /**
     * Interface for scripts in JVM languages to parse JSON strings into
     * objects relevant to their use cases. 
     */
    public interface JsonParseFunction {

        /**
         * Parses json string into some object. Anytime {@link ProcessCodeGenericTask} needs
         * to parse a JSON string, this method will be called.
         * @param json JSON string
         * @return parsed object
         * @throws Exception
         */
        Object parse(String json) throws Exception;
    }

    /**
     * Interface to code evaluation facility in JVM scripting language.
     */
    public interface EvalFunction {

        /**
         * Invokes code evaluation facility in a scripting language context 
         * to produce generated code object corresponding to an augmenting code object.
         * @param functionName trimmed content of first line of current augmenting code object.
         * Although script can do anything it wants to with current augmenting code object,
         * it is strongly recommended that data driven programming paradigm be followed.
         * In that case this parameter refers to name of a function code evaluation facility should 
         * call with (at least) 2 arguments: current augmenting code object and a 
         * helper context object.
         * @param augCode current augmenting code object
         * @param context helper context object available for use by script
         * @return {@link GeneratedCode} instance or an array of such instances.
         * {@link ContentPart} instances can be returned as well. An instance of any other
         * class will have toString() called on it and used as a content part with inexact
         * matching.
         * @throws Exception
         */
        Object apply(String functionName, AugmentingCode augCode,
            ProcessCodeContext context) throws Exception;
    }

    /**
     * Used to validate arguments passed to {@link EvalFunction}
     */
    public interface ValidationCallback {
        /**
         * Validates arguments of {@link EvalFunction#apply(String, AugmentingCode, ProcessCodeContext)}
         * @param functionName
         * @param augCode
         * @param context
         * @return validation error message or null to signal validation success
         * @throws Exception
         */
        String validate(String functionName, AugmentingCode augCode,
            ProcessCodeContext context) throws Exception;
    }

    // input properties
    private BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender;
    private File inputFile;
    private File outputFile;
    private JsonParseFunction jsonParseFunction;
    private ValidationCallback validationCallback;
    private final ValidationCallback defaultValidationCallback;
    private EvalFunction evalFunction;

    // output properties
    private final List<Throwable> allErrors = new ArrayList<>();

    public ProcessCodeGenericTask() {
        defaultValidationCallback = (f, a, c) -> {
            if (f.startsWith("context.")) {
                return null;
            }
            return "Expected method call on context helper object, but got: " + f;
        };
    }

    /**
     * Executes processing stage of Code Augmentor. Augmenting code objects
     * will be read from input file, passed to eval mechanism of a 
     * script, and the resulting generated code objects will be written out
     * to output file.
     * <p>
     * Success of this operation depends on emptiness of allErrors property.
     * @throws Exception
     */
    public void execute() throws Exception {
        // validate input properties
        Objects.requireNonNull(inputFile, "inputFile property is not set");
        Objects.requireNonNull(outputFile, "outputFile property is not set");
        Objects.requireNonNull(jsonParseFunction, "jsonParseFunction property is not set");
        Objects.requireNonNull(validationCallback, "validationCallback property is not set");
        Objects.requireNonNull(evalFunction, "evalFunction property is not set");

        allErrors.clear();
        // ensure dir exists for outputFile
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

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
                        }
                        else if (block.isStringify()) {
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
                    List<GeneratedCode> genCodes = processAugCode(functionName, augCode, context,
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
    List<GeneratedCode> processAugCode(
            String functionName, AugmentingCode augCode, ProcessCodeContext context, 
            List<Throwable> errors) {
        try {
            String validationError = (validationCallback != null ?
                    validationCallback : defaultValidationCallback).validate(functionName, 
                augCode, context);
            if (validationError != null) {                    
                GenericTaskException evalEx = createException(context,
                    "Invalid arguments to evalFunction: " + validationError, null);
                errors.add(evalEx);
                return Arrays.asList();
            }

            Object result = evalFunction.apply(functionName, augCode, context);
    
            // Convert various alternative representations of List<GeneratedCode>     
            if (result == null) {
                return Arrays.asList(convertGenCodeItem(null));
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
        List<Integer> validIds = ids.stream().filter(x -> x > 0).collect(Collectors.toList());
        if (ids.stream().anyMatch(x -> x == 0)) {
            errors.add(createException(context,
                "At least one generated code id was not set. Found: " +
                    ids, null));
        }
        else if (validIds.stream().distinct().count() < validIds.size()) {
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

    /**
     * Sets logging procedure for this task. By default this property is null, 
     * and so no logging is done.
     * @param logAppender logging procedure or null for no logging
     */
    public void setLogAppender(BiConsumer<GenericTaskLogLevel, Supplier<String>> logAppender) {
        this.logAppender = logAppender;
    }

    /**
     * Gets the input file to read augmenting code objects from.
     * Corresponds to {@link AugCodeProcessingSpec#getDestFile()}
     * @return input file for task.
     */
    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Gets the output file to write generated code objects to.
     * Corresponds to {@link CodeAugmentationGenericTask#getGeneratedCodeFiles()}
     * @return output file of this task.
     */
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public JsonParseFunction getJsonParseFunction() {
        return jsonParseFunction;
    }

    /**
     * Sets the function object used to parse JSON strings into objects convenient
     * for processing by a JVM language script.
     * @param jsonParseFunction
     */
    public void setJsonParseFunction(JsonParseFunction jsonParseFunction) {
        this.jsonParseFunction = jsonParseFunction;
    }

    public ValidationCallback getValidationCallback() {
        return validationCallback;
    }

    /**
     * Sets procedure which will be used to validate function names before 
     * script evaluation.
     * @param validationCallback
     */
    public void setValidationCallback(ValidationCallback validationCallback) {
        this.validationCallback = validationCallback;
    }

    /**
     * Gets default validation callback which will be used if none is set with
     * {@link #setValidationCallback(ValidationCallback)}
     * @return
     */
    public ValidationCallback getDefaultValidationCallback() {
        return defaultValidationCallback;
    }

    public EvalFunction getEvalFunction() {
        return evalFunction;
    }

    /**
     * Sets interface into a script's code evaluation
     * facility which will be called to produce generated code objects corresponding
     * to augmenting code objects in input file.
     * @param evalFunction
     */
    public void setEvalFunction(EvalFunction evalFunction) {
        this.evalFunction = evalFunction;
    }

    /**
     * Gets the error results of executing this task.
     * @return empty list if task execution was successful; non-empty list if
     * task execution failed.
     */
    public List<Throwable> getAllErrors() {
        return allErrors;
    }
}