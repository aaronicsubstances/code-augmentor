package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.json.JsonSlurper;

public class ProcessCodeTask extends DefaultTask {
    private int augCodeSpecIndex = 0;
    private int genCodeFileIndex = 0;
    private final Property<Object> scriptDir;
    private final Property<String> entryScriptName;

    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    public ProcessCodeTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        scriptDir = objectFactory.property(Object.class);
        entryScriptName = objectFactory.property(String.class);
    }

    @TaskAction    
    public void execute() throws Exception {
        try {
            CodeAugmentorPluginExtension ext = getProject().getExtensions().findByType(
                CodeAugmentorPluginExtension.class);
            List<AugCodeDirectiveSpec> augCodeSpecs = ext.getAugCodeSpecs().get();
            AugCodeDirectiveSpec augCodeSpec = augCodeSpecs.get(augCodeSpecIndex);
            File resolvedAugCodeFile = getProject().file(augCodeSpec.getDestFile());
            List<Object> genCodeFiles = ext.getGeneratedCodeFiles().get();
            Object genCodeFile = genCodeFiles.get(genCodeFileIndex);
            File resolvedGenCodeFile = getProject().file(genCodeFile);
    
            if (!scriptDir.isPresent()) {
                throw new GradleException("scriptDir property must be set");
            }

            File resolvedScriptDir = getProject().file(scriptDir.get());
            String mainScriptName = entryScriptName.get();
    
            ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
            genericTask.setLogAppender(TaskUtils.createLogAppender(this));
            genericTask.setInputFile(resolvedAugCodeFile);
            genericTask.setOutputFile(resolvedGenCodeFile);
            genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));
    
            URL[] scriptEngineRoots = new URL[]{ resolvedScriptDir.toURI().toURL() };
            GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
            Binding binding = new Binding();
            binding.setVariable("parentTask", genericTask);

            if (ext.getVerbose().get()) {
                // print task properties - generic task ones, and any ones outside
                getLogger().info("Configuration properties:");
                getLogger().info("\taugCodeSpecIndex: " + augCodeSpecIndex);
                getLogger().info("\tgenCodeFileIndex: " + genCodeFileIndex);
                getLogger().info("\tscriptDir: " + resolvedScriptDir);
                getLogger().info("\tentryScriptName: " + mainScriptName);
                getLogger().info("\tgenericTask.inputFile: " + resolvedAugCodeFile);
                getLogger().info("\tgenericTask.outputFile: " + resolvedGenCodeFile);
                getLogger().info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
                getLogger().info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
            }

            getLogger().info("Launching " + mainScriptName + "...");
            List<Throwable> scriptErrors = new ArrayList<>();
            try {
                scriptEngine.run(mainScriptName, binding);
            }
            catch (Throwable t) {
                scriptErrors.add(t);
            }

            scriptErrors.addAll(genericTask.getAllErrors());
    
            // fail build if there were errors.
            if (!scriptErrors.isEmpty()) {
                throw TaskUtils.convertToGradleException(scriptErrors);
            }
        }
        catch (Throwable ex) {
            if (ex instanceof GradleException) {
                throw ex;
            }
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }

    @Internal
    public int getAugCodeSpecIndex() {
        return augCodeSpecIndex;
    }

    public void setAugCodeSpecIndex(int augCodeSpecIndex) {
        this.augCodeSpecIndex = augCodeSpecIndex;
    }

    @Internal
    public int getGenCodeFileIndex() {
        return genCodeFileIndex;
    }

    public void setGenCodeFileIndex(int genCodeFileIndex) {
        this.genCodeFileIndex = genCodeFileIndex;
    }

    @Internal
	public Property<Object> getScriptDir() {
		return scriptDir;
	}

    @Internal
    public Property<String> getEntryScriptName() {
        return entryScriptName;
    }
}