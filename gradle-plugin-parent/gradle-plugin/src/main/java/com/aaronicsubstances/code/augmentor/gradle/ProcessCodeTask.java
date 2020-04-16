package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.net.URL;
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
    private final Property<Object> scriptsDir;
    private final Property<String> entryScriptName;

    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    public ProcessCodeTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        scriptsDir = objectFactory.property(Object.class);
        entryScriptName = objectFactory.property(String.class);
    }

    @TaskAction    
    public void execute() throws Exception {
        try {
            getLogger().info("Beginning execute()...");
            CodeAugmentorPluginExtension ext = getProject().getExtensions().findByType(CodeAugmentorPluginExtension.class);
            List<AugCodeDirectiveSpec> augCodeSpecs = ext.getAugCodeSpecs().get();
            AugCodeDirectiveSpec augCodeSpec = augCodeSpecs.get(augCodeSpecIndex);
            File resolvedAugCodeFile = getProject().file(augCodeSpec.getDestFile());
            List<Object> genCodeFiles = ext.getGeneratedCodeFiles().get();
            Object genCodeFile = genCodeFiles.get(genCodeFileIndex);
            File resolvedGenCodeFile = getProject().file(genCodeFile);
    
            if (!scriptsDir.isPresent()) {
                throw new GradleException("scriptsDir property must be set");
            }

            File resolvedScriptsDir = getProject().file(scriptsDir.get());
            String mainScriptName = entryScriptName.get();
    
            ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
            genericTask.setLogAppender(TaskUtils.createLogAppender(this));
            genericTask.setInputFile(resolvedAugCodeFile);
            genericTask.setOutputFile(resolvedGenCodeFile);
            genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));
    
            URL[] scriptEngineRoots = new URL[]{ resolvedScriptsDir.toURI().toURL() };
            GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
            Binding binding = new Binding();
            binding.setVariable("parentTask", genericTask);
            scriptEngine.run(mainScriptName, binding);
            getLogger().info("Completed execute().");
    
            // fail build if there were errors.
            if (!genericTask.getAllErrors().isEmpty()) {
                throw TaskUtils.convertToGradleException(genericTask.getAllErrors());
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
	public Property<Object> getScriptsDir() {
		return scriptsDir;
	}

    @Internal
    public Property<String> getEntryScriptName() {
        return entryScriptName;
    }
}