package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Meant for use by embedding ANT library, rather than invoking ANT command line
 * tool.
 */
public class CodeAugmentorTask extends Task {
    public static final String PROJECT_REFERENCE_JSON_PARSE_FUNCTION = 
        "codeAugmentor.jsonParseFunction";
    public static final String PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION = 
        "codeAugmentor.scriptEvalFunction";
    public static String PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES = 
        "codeAugmentor.defaultStackTraceLimitPrefixes";

    private String encoding;
    private boolean verbose;
    private final List<FileSet> srcDirs = new ArrayList<>();
    private final List<String> genCodeStartDirectives = new ArrayList<>();
    private final List<String> genCodeEndDirectives = new ArrayList<>();
    private final List<String> embeddedStringDirectives = new ArrayList<>();
    private final List<String> embeddedJsonDirectives = new ArrayList<>();
    private final List<String> skipCodeStartDirectives = new ArrayList<>();
    private final List<String> skipCodeEndDirectives = new ArrayList<>();
    private final List<String> augCodeDirectives = new ArrayList<>();
    private final List<String> inlineGenCodeDirectives = new ArrayList<>();
    private final List<String> nestedLevelStartMarkers = new ArrayList<>();
    private final List<String> nestedLevelEndMarkers = new ArrayList<>();
    
    private String stackTraceLimitPrefixes;
    private String stackTraceFilterPrefixes;

    private File destDir;
    private boolean codeChangeDetectionDisabled;
    private boolean failOnChanges = true;

    private File prepFile;
    private File augCodeFile;
    private File genCodeFile;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void addSrcDir(FileSet f) {
        srcDirs.add(f);
    }

    public void addConfiguredGenCodeStartDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        genCodeStartDirectives.add(val);
    }

    public void addConfiguredGenCodeEndDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        genCodeEndDirectives.add(val);
    }

    public void addConfiguredEmbeddedStringDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        embeddedStringDirectives.add(val);
    }

    public void addConfiguredEmbeddedJsonDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        embeddedJsonDirectives.add(val);
    }

    public void addConfiguredSkipCodeStartDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        skipCodeStartDirectives.add(val);
    }

    public void addConfiguredSkipCodeEndDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        skipCodeEndDirectives.add(val);
    }

    public void addConfiguredInlineGenCodeDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        inlineGenCodeDirectives.add(val);
    }

    public void addConfiguredNestedLevelStartMarker(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        nestedLevelStartMarkers.add(val);
    }

    public void addConfiguredNestedLevelEndMarker(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        nestedLevelEndMarkers.add(val);
    }

    public void addConfiguredAugCodeDirective(Directive d) {
        String val = null;
        if (d.getValue() != null) {
            val = d.getValue();
        }
        augCodeDirectives.add(val);
    }

    public void setStackTraceLimitPrefixes(String stackTraceLimitPrefixes) {
        this.stackTraceLimitPrefixes = stackTraceLimitPrefixes;
    }

    public void setStackTraceFilterPrefixes(String stackTraceFilterPrefixes) {
        this.stackTraceFilterPrefixes = stackTraceFilterPrefixes;
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
    
    public void setCodeChangeDetectionDisabled(boolean codeChangeDetectionDisabled) {
        this.codeChangeDetectionDisabled = codeChangeDetectionDisabled;
    }

    public void setFailOnChanges(boolean failOnChanges) {
        this.failOnChanges = failOnChanges;
    }

    public void setPrepFile(File prepFile) {
        this.prepFile = prepFile;
    }

    public void setAugCodeFile(File augCodeFile) {
        this.augCodeFile = augCodeFile;
    }

    public void setGenCodeFile(File genCodeFile) {
        this.genCodeFile = genCodeFile;
    }

    public void execute() {
        try {
            // prepare...
            // only add aug code directives if there's something specified by
            // user, so that default can be added later on.
            List<List<String>> resolvedAugCodeDirectives = new ArrayList<>();
            if (!augCodeDirectives.isEmpty()) {
                resolvedAugCodeDirectives.add(augCodeDirectives);
            }
            List<File> augCodeFiles = new ArrayList<>();
            if (augCodeFile != null) {
                augCodeFiles.add(augCodeFile);
            }
            PrepareTask.completeExecute(this, encoding, verbose, srcDirs, 
                genCodeStartDirectives, genCodeEndDirectives, 
                embeddedStringDirectives, embeddedJsonDirectives, 
                skipCodeStartDirectives, skipCodeEndDirectives, 
                resolvedAugCodeDirectives, 
                augCodeFiles,
                prepFile,
                inlineGenCodeDirectives,
                nestedLevelStartMarkers, nestedLevelEndMarkers);

            // process...
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
            
            executeProcessStage(verbose, augCodeFile, genCodeFile, 
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);

            // complete.
            List<File> genCodeFiles = new ArrayList<>();
            if (genCodeFile != null) {
                genCodeFiles.add(genCodeFile);
            }
            CompletionTask.completeExecute(this, verbose, prepFile,
                genCodeFiles, destDir, codeChangeDetectionDisabled, failOnChanges);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void executeProcessStage(boolean resolvedVerbose,
            File resolvedAugCodeFile, File resolvedGenCodeFile,
            List<String> resolvedStackTraceLimitPrefixes, 
            List<String> resolvedStackTraceFilterPrefixes) throws Exception {
        // set up defaults
        if (resolvedAugCodeFile == null) {
            resolvedAugCodeFile = TaskUtils.getDefaultAugCodeFile(this);
        }
        if (resolvedGenCodeFile == null) {
            resolvedGenCodeFile = TaskUtils.getDefaultGenCodeFile(this);
        }
        
        // json parse function is required.            
        ProcessCodeGenericTask.JsonParseFunction resolvedJsonParseFunction = 
            (ProcessCodeGenericTask.JsonParseFunction) getProject().getReference(
                PROJECT_REFERENCE_JSON_PARSE_FUNCTION);
        if (resolvedJsonParseFunction == null) {
            throw new BuildException(PROJECT_REFERENCE_JSON_PARSE_FUNCTION +
                " reference is not set");
        }

        // eval function is required.            
        ProcessCodeGenericTask.EvalFunction resolvedScriptEvalFunction = 
            (ProcessCodeGenericTask.EvalFunction) getProject().getReference(
                PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION);
        if (resolvedScriptEvalFunction == null) {
            throw new BuildException(PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION +
                " reference is not set");
        }

        // make use of default stack trace limit prefix if present
        if (resolvedStackTraceLimitPrefixes == null ||
                resolvedStackTraceLimitPrefixes.isEmpty()) {
            List<String> defaultStackTraceLimitPrefixes =
                (List<String>) getProject().getReference(
                    PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES);
            resolvedStackTraceLimitPrefixes = defaultStackTraceLimitPrefixes;
        }

        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(this, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(resolvedJsonParseFunction);

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            log("Configuration properties:");
            log("\taugCodeFile: " + resolvedAugCodeFile);
            log("\tgenCodeFile: " + resolvedGenCodeFile);
            log("\t" + PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION +
                 " reference: " + resolvedScriptEvalFunction);
            log("\tstackTraceLimitPrefixes: " + resolvedStackTraceLimitPrefixes);
            log("\tstackTraceFilterPrefixes: " + resolvedStackTraceFilterPrefixes);
            log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            log("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
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
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                scriptErrors, true, 
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);
            throw new BuildException(allExMsg);
        }
    }
}