package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.util.GroovyScriptEngine;

public class ProcessCodeTask extends DefaultTask {
    private int augCodeSpecIndex = 0;
    private int genCodeFileIndex = 0;
    private final Property<Object> scriptsDir;
    private final Property<String> entryScriptName;

    // saved for use during Groovy script execution
    private File resolvedAugCodeFile;
    private File resolvedGenCodeFile;
    
    public ProcessCodeTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        scriptsDir = objectFactory.property(Object.class);
        entryScriptName = objectFactory.property(String.class);
    }

    @TaskAction    
    public void execute() throws Exception {
        try {
            _execute();
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new GradleException("General plugin error: " + ex.getMessage(), ex);
        }
    }

    private void _execute() throws Exception {
        CodeAugmentorPluginExtension ext = getProject().getExtensions().findByType(CodeAugmentorPluginExtension.class);
        List<AugCodeDirectiveSpec> augCodeSpecs = ext.getAugCodeSpecs().get();
        AugCodeDirectiveSpec augCodeSpec = augCodeSpecs.get(augCodeSpecIndex);
        resolvedAugCodeFile = getProject().file(augCodeSpec.getDestFile());
        List<Object> genCodeFiles = ext.getGeneratedCodeFiles().get();
        Object genCodeFile = genCodeFiles.get(genCodeFileIndex);
        resolvedGenCodeFile = getProject().file(genCodeFile);

        if (!scriptsDir.isPresent()) {
            throw new GradleException("scriptsDir property must be set");
        }
        File resolvedScriptsDir = getProject().file(scriptsDir.get());
        URL[] scriptEngineRoots = new URL[]{ resolvedScriptsDir.toURI().toURL() };
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
        Binding binding = new Binding();
        binding.setVariable("parentTask", this);
        scriptEngine.run(entryScriptName.get(), binding);
    }

    public void completeExecute(Closure<?> functionCallClosure) throws Exception {
        if (functionCallClosure == null) {
            throw new GradleException("Groovy script failed to provide code for processing aug codes is null");
        }
        ProcessCodeTaskDelegate delegate = new ProcessCodeTaskDelegate(functionCallClosure);
        delegate.execute(resolvedAugCodeFile, resolvedGenCodeFile);
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