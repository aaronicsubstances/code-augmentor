package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskExtensionFunction;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.json.JsonSlurper;

public class ProcessingTask extends DefaultTask {
    private final Property<Boolean> verbose;
    private final ListProperty<AugCodeDirectiveSpec> augCodeSpecs;
    private final Property<Integer> augCodeSpecIndex;
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Integer> genCodeFileIndex;
    private final Property<Object> groovyScriptDir;
    private final Property<String> groovyEntryScriptName;
    private final Property<GenericTaskExtensionFunction> scriptEvalFunction;
    private final ListProperty<String> scriptErrorStackTraceFilterPrefixes;
    private final ListProperty<String> scriptErrorStackTraceLimitPrefixes;
    
    public ProcessingTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        verbose = objectFactory.property(Boolean.class);
        augCodeSpecs = objectFactory.listProperty(AugCodeDirectiveSpec.class);
        augCodeSpecIndex = objectFactory.property(Integer.class);
        generatedCodeFiles = objectFactory.listProperty(Object.class);
        genCodeFileIndex = objectFactory.property(Integer.class);
        groovyScriptDir = objectFactory.property(Object.class);
        groovyEntryScriptName = objectFactory.property(String.class);
        scriptEvalFunction = objectFactory.property(GenericTaskExtensionFunction.class);
        scriptErrorStackTraceFilterPrefixes = objectFactory.listProperty(String.class);
        scriptErrorStackTraceLimitPrefixes = objectFactory.listProperty(String.class);
    }

    @TaskAction
    public void execute() throws GradleException {
        try {
            boolean resolvedVerbose = verbose.get();
            int resolvedAugCodeSpecIndex = augCodeSpecIndex.get();
            List<AugCodeDirectiveSpec> resolvedAugCodeSpecs = augCodeSpecs.get();
            if (resolvedAugCodeSpecIndex < 0 || resolvedAugCodeSpecIndex >= resolvedAugCodeSpecs.size()) {
                throw new GradleException(String.format("Invalid augCodeSpecIndex lies outside valid " +
                    "range of 0 ..< %s: %s", resolvedAugCodeSpecs.size(), resolvedAugCodeSpecIndex));
            }
            AugCodeDirectiveSpec augCodeSpec = resolvedAugCodeSpecs.get(resolvedAugCodeSpecIndex);
            File resolvedAugCodeFile = null;
            if (augCodeSpec != null) {
                resolvedAugCodeFile = getProject().file(augCodeSpec.getDestFile());
            }
            int resolvedGenCodeFileIndex = genCodeFileIndex.get();
            List<Object> resolvedGenCodeFiles = generatedCodeFiles.get();
            if (resolvedGenCodeFileIndex < 0 || resolvedGenCodeFileIndex >= resolvedGenCodeFiles.size()) {
                throw new GradleException(String.format("Invalid genCodeFileIndex lies outside valid " +
                    "range of 0 ..< %s: %s", resolvedGenCodeFiles.size(), resolvedGenCodeFileIndex));
            }
            Object genCodeFile = resolvedGenCodeFiles.get(resolvedGenCodeFileIndex);
            File resolvedGenCodeFile = getProject().file(genCodeFile);
            
            GenericTaskExtensionFunction resolvedScriptEvalFunction = null;
            if (scriptEvalFunction.isPresent()) {
                resolvedScriptEvalFunction = scriptEvalFunction.get();
            }
            File resolvedGroovyScriptDir = null;
            if (groovyScriptDir.isPresent()) {
                resolvedGroovyScriptDir = getProject().file(groovyScriptDir);
            }
            String resolvedGroovyEntryScriptName = groovyEntryScriptName.get();
            
            List<String> resolvedStackTraceLimitPrefixes = scriptErrorStackTraceLimitPrefixes.get();
            List<String> resolvedStackTraceFilterPrefixes = scriptErrorStackTraceFilterPrefixes.get();
            completeExecute(this, resolvedVerbose, resolvedAugCodeSpecIndex, 
                resolvedGenCodeFileIndex, resolvedAugCodeFile, resolvedGenCodeFile,
                resolvedScriptEvalFunction, resolvedStackTraceLimitPrefixes,
                resolvedStackTraceFilterPrefixes, resolvedGroovyScriptDir,
                resolvedGroovyEntryScriptName);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }

//:SKIP_CODE_START:
    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    static void completeExecute(DefaultTask task, boolean resolvedVerbose,
            int resolvedAugCodeSpecIndex, int resolvedGenCodeFileIndex,
            File resolvedAugCodeFile, File resolvedGenCodeFile, 
            GenericTaskExtensionFunction resolvedScriptEvalFunction,
            List<String> resolvedStackTraceLimitPrefixes, 
            List<String> resolvedStackTraceFilterPrefixes,
            File resolvedGroovyScriptDir, String resolvedGroovyEntryScriptName) throws Exception {
        
        if (resolvedAugCodeFile == null) {
            if (task instanceof ProcessingTask) {
                int i = resolvedAugCodeSpecIndex;
                throw new GradleException("invalid null value found at augCodeSpecs[" + i + "]?.destFile");
            }
            else {
                throw new RuntimeException("unexpected absence of augCodeFile");
            }
        }
        if (resolvedGenCodeFile == null) {
            if (task instanceof ProcessingTask) {
                int i = resolvedGenCodeFileIndex;
                throw new GradleException("invaid null value found at generatedCodeFiles[" + i + "]");
            }
            else {
                throw new RuntimeException("unexpected absence of genCodeFile");
            }
        }
        // either eval function or groovy script dir is required.
        if (resolvedScriptEvalFunction == null && resolvedGroovyScriptDir == null) {
            throw new GradleException("groovyScriptDir property must be set if scriptEvalFunction is absent");
        }
        Logger logger = task.getLogger();
        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(task, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            if (task instanceof ProcessingTask) {
                logger.info("\taugCodeSpecIndex: " + resolvedAugCodeSpecIndex);
                logger.info("\tgenCodeFileIndex: " + resolvedGenCodeFileIndex);
            }
            logger.info("\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            logger.info("\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            logger.info("\tscriptEvalFunction: " + resolvedScriptEvalFunction);
            logger.info("\tstackTraceLimitPrefixes: " + resolvedStackTraceLimitPrefixes);
            logger.info("\tstackTraceFilterPrefixes: " + resolvedStackTraceFilterPrefixes);
            logger.info("\tgenericTask.inputFile: " + resolvedAugCodeFile);
            logger.info("\tgenericTask.outputFile: " + resolvedGenCodeFile);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            logger.info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
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
            logger.info("Launching " + resolvedGroovyEntryScriptName + "...");
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

    @Internal
    public Property<Integer> getAugCodeSpecIndex() {
        return augCodeSpecIndex;
    }

    @Internal
    public Property<Integer> getGenCodeFileIndex() {
        return genCodeFileIndex;
    }

    @Internal
    public Property<GenericTaskExtensionFunction> getScriptEvalFunction() {
        return scriptEvalFunction;
    }

    @Internal
    public ListProperty<String> getScriptErrorStackTraceFilterPrefixes() {
        return scriptErrorStackTraceFilterPrefixes;
    }

    @Internal
    public ListProperty<String> getScriptErrorStackTraceLimitPrefixes() {
        return scriptErrorStackTraceLimitPrefixes;
    }

    @Internal
	public Property<Object> getGroovyScriptDir() {
		return groovyScriptDir;
	}

    @Internal
    public Property<String> getGroovyEntryScriptName() {
        return groovyEntryScriptName;
    }

    @Internal
    public Property<Boolean> getVerbose() {
        return verbose;
    }

    @Internal
    public ListProperty<AugCodeDirectiveSpec> getAugCodeSpecs() {
        return augCodeSpecs;
    }

    @Internal
    public ListProperty<Object> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }
}