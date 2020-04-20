package com.aaronicsubstances.code.augmentor.ant;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskExtensionFunction;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

public class ProcessTask extends Task {
    private boolean verbose;
    private File augCodeFile;
    private File genCodeFile;
    private GenericTaskExtensionFunction scriptEvalFunction;
    private String stackTraceLimitPrefixes;
    private String stackTraceFilterPrefixes;
    private File groovyScriptDir;
    private String groovyEntryScriptName;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setAugCodeFile(File augCodeFile) {
        this.augCodeFile = augCodeFile;
    }

    public void setGenCodeFile(File genCodeFile) {
        this.genCodeFile = genCodeFile;
    }

    public void setScriptEvalFunction(GenericTaskExtensionFunction scriptEvalFunction) {
        this.scriptEvalFunction = scriptEvalFunction;
    }

    public void setStackTraceLimitPrefixes(String stackTraceLimitPrefixes) {
        this.stackTraceLimitPrefixes = stackTraceLimitPrefixes;
    }

    public void setStackTraceFilterPrefixes(String stackTraceFilterPrefixes) {
        this.stackTraceFilterPrefixes = stackTraceFilterPrefixes;
    }

    public void setGroovyScriptDir(File groovyScriptDir) {
        this.groovyScriptDir = groovyScriptDir;
    }

    public void setGroovyEntryScriptName(String groovyEntryScriptName) {
        this.groovyEntryScriptName = groovyEntryScriptName;
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
            if (resolvedStackTraceFilterPrefixes != null) {                
                resolvedStackTraceFilterPrefixes = Arrays.stream(stackTraceFilterPrefixes.split(",", -1))
                    .map(x -> x.trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
            }
            
            completeExecute(this, verbose, 0, 0, augCodeFile, genCodeFile, 
                scriptEvalFunction, resolvedStackTraceLimitPrefixes, 
                resolvedStackTraceFilterPrefixes, groovyScriptDir, 
                groovyEntryScriptName);
        }
        catch (BuildException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BuildException("General error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    static void completeExecute(Task task, boolean resolvedVerbose,
            int resolvedAugCodeSpecIndex, int resolvedGenCodeFileIndex,
            File resolvedAugCodeFile, File resolvedGenCodeFile, 
            GenericTaskExtensionFunction resolvedScriptEvalFunction,
            List<String> resolvedStackTraceLimitPrefixes, 
            List<String> resolvedStackTraceFilterPrefixes,
            File resolvedGroovyScriptDir, String resolvedGroovyEntryScriptName) throws Exception {
        // set up defaults
        if (resolvedAugCodeFile == null) {
            resolvedAugCodeFile = TaskUtils.getDefaultAugCodeFile(task);
        }
        if (resolvedGenCodeFile == null) {
            resolvedGenCodeFile = TaskUtils.getDefaultGenCodeFile(task);
        }
        if (resolvedGroovyEntryScriptName == null) {
            resolvedGroovyEntryScriptName = "main.groovy";
        }
        // either eval function or groovy script dir is required.
        if (resolvedScriptEvalFunction == null && resolvedGroovyScriptDir == null) {
            throw new BuildException("groovyScriptDir property is required if scriptEvalFunction is absent");
        }
        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            task.log("Configuration properties:");
            task.log("\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            task.log("\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            task.log("\taugCodeFile: " + resolvedAugCodeFile);
            task.log("\tgenCodeFile: " + resolvedGenCodeFile);
            task.log("\tscriptEvalFunction: " + resolvedScriptEvalFunction);
            task.log("\tstackTraceLimitPrefixes: " + resolvedStackTraceLimitPrefixes);
            task.log("\tstackTraceFilterPrefixes: " + resolvedStackTraceFilterPrefixes);
            task.log("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            task.log("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
        }

        List<Throwable> scriptErrors = new ArrayList<>();
        boolean defaultGroovyUsed = false;
        if (resolvedScriptEvalFunction == null) {
            defaultGroovyUsed = true;
            URL[] scriptEngineRoots = new URL[]{ resolvedGroovyScriptDir.toURI().toURL() };
            GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
            CompilerConfiguration cc = new CompilerConfiguration();
            cc.setRecompileGroovySource(false);
            scriptEngine.setConfig(cc);
            Binding binding = new Binding();
            binding.setVariable("parentTask", genericTask);
            task.log("Launching " + resolvedGroovyEntryScriptName + "...");
            scriptErrors = new ArrayList<>();
            try {
                scriptEngine.run(resolvedGroovyEntryScriptName, binding);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }
        }
        else {
            try {
                genericTask.execute(resolvedScriptEvalFunction);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }
        }

        scriptErrors.addAll(genericTask.getAllErrors());

        // fail build if there were errors.
        if (!scriptErrors.isEmpty()) {
            throw TaskUtils.convertToPluginException(scriptErrors, true, defaultGroovyUsed,
                resolvedStackTraceLimitPrefixes, resolvedStackTraceFilterPrefixes);
        }
    }
//:SKIP_CODE_END:
}