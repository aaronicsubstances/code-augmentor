package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskException;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ProcessTask extends Task {
    public static final String PROJECT_REFERENCE_JSON_PARSE_FUNCTION = 
        "codeAugmentor.jsonParseFunction";
    public static final String PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION = 
        "codeAugmentor.scriptEvalFunction";
    public static String PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES = 
        "codeAugmentor.defaultStackTraceLimitPrefixes";

    private boolean verbose;
    private File augCodeFile;
    private File genCodeFile;
    private String stackTraceLimitPrefixes;
    private String stackTraceFilterPrefixes;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setAugCodeFile(File augCodeFile) {
        this.augCodeFile = augCodeFile;
    }

    public void setGenCodeFile(File genCodeFile) {
        this.genCodeFile = genCodeFile;
    }

    public void setStackTraceLimitPrefixes(String stackTraceLimitPrefixes) {
        this.stackTraceLimitPrefixes = stackTraceLimitPrefixes;
    }

    public void setStackTraceFilterPrefixes(String stackTraceFilterPrefixes) {
        this.stackTraceFilterPrefixes = stackTraceFilterPrefixes;
    }

    public void execute() {
        try {
            List<String> resolvedStackTraceLimitPrefixes = new ArrayList<>();
            if (stackTraceLimitPrefixes != null) {
                resolvedStackTraceLimitPrefixes = Arrays.stream(stackTraceLimitPrefixes.split(",", -1))
                    .map(x -> x.trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
            List<String> resolvedStackTraceFilterPrefixes = new ArrayList<>();
            if (stackTraceFilterPrefixes != null) {                
                resolvedStackTraceFilterPrefixes = Arrays.stream(stackTraceFilterPrefixes.split(",", -1))
                    .map(x -> x.trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
                
            completeExecute(this, verbose, 0, 0, augCodeFile, genCodeFile, 
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    static void completeExecute(Task task, boolean resolvedVerbose,
            int resolvedAugCodeSpecIndex, int resolvedGenCodeFileIndex,
            File resolvedAugCodeFile, File resolvedGenCodeFile,
            List<String> resolvedStackTraceLimitPrefixes, 
            List<String> resolvedStackTraceFilterPrefixes) throws Exception {
        // set up defaults
        if (resolvedAugCodeFile == null) {
            resolvedAugCodeFile = TaskUtils.getDefaultAugCodeFile(task);
        }
        if (resolvedGenCodeFile == null) {
            resolvedGenCodeFile = TaskUtils.getDefaultGenCodeFile(task);
        }
        
        // json parse function is required.            
        ProcessCodeGenericTask.JsonParseFunction resolvedJsonParseFunction = 
            (ProcessCodeGenericTask.JsonParseFunction) task.getProject().getReference(
                PROJECT_REFERENCE_JSON_PARSE_FUNCTION);
        if (resolvedJsonParseFunction == null) {
            throw new BuildException(PROJECT_REFERENCE_JSON_PARSE_FUNCTION +
                " reference is not set");
        }

        // eval function is required.            
        ProcessCodeGenericTask.EvalFunction resolvedScriptEvalFunction = 
            (ProcessCodeGenericTask.EvalFunction) task.getProject().getReference(
                PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION);
        if (resolvedScriptEvalFunction == null) {
            throw new BuildException(PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION +
                " reference is not set");
        }

        // make use of default stack trace limit prefix if present
        if (resolvedStackTraceLimitPrefixes == null ||
                resolvedStackTraceLimitPrefixes.isEmpty()) {
            List<String> defaultStackTraceLimitPrefixes =
                (List<String>) task.getProject().getReference(
                    PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES);
            resolvedStackTraceLimitPrefixes = defaultStackTraceLimitPrefixes;
        }

        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(resolvedJsonParseFunction);

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            task.log("Configuration properties:");
            task.log("\taugCodeFile: " + resolvedAugCodeFile);
            task.log("\tgenCodeFile: " + resolvedGenCodeFile);
            task.log("\t" + PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION +
                 " reference: " + resolvedScriptEvalFunction);
            task.log("\tstackTraceLimitPrefixes: " + resolvedStackTraceLimitPrefixes);
            task.log("\tstackTraceFilterPrefixes: " + resolvedStackTraceFilterPrefixes);
            task.log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
        }

        List<Throwable> scriptErrors = new ArrayList<>();
        try {
            genericTask.execute(resolvedScriptEvalFunction);
        }
        catch (Throwable t) {
            scriptErrors.add(t);
        }

        scriptErrors.addAll(genericTask.getAllErrors());

        // fail build if there were errors.
        if (!scriptErrors.isEmpty()) {
            String allExMsg = GenericTaskException.toExceptionMessageWithScriptConsideration(
                scriptErrors, true, 
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);
            throw new BuildException(allExMsg);
        }
    }
}