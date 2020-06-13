package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.tasks.PluginUtils;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

public class CodeAugmentorTask extends DefaultTask {
    private final Property<Boolean> verbose;
    private final Property<String> encoding;
    private final ListProperty<ConfigurableFileTree> fileSets;
    private final ListProperty<String> augCodeDirectives;
    private final ListProperty<String> genCodeStartDirectives;
    private final ListProperty<String> genCodeEndDirectives;
    private final ListProperty<String> embeddedStringDirectives;
    private final ListProperty<String> embeddedJsonDirectives;
    private final ListProperty<String> skipCodeStartDirectives;
    private final ListProperty<String> skipCodeEndDirectives;
    private final ListProperty<String> inlineGenCodeDirectives;
    private final ListProperty<String> nestedLevelStartMarkers;
    private final ListProperty<String> nestedLevelEndMarkers;
    
    private final Property<Object> groovyScriptDir;
    private final Property<String> groovyEntryScriptName;

    private final Property<Object> destDir;
    private final Property<Boolean> codeChangeDetectionDisabled;
    private final Property<Boolean> failOnChanges;

    private final Property<Object> prepFile;
    private final Property<Object> augCodeFile;
    private final Property<Object> genCodeFile;

    private static final JsonSlurper JSON_PARSER = new JsonSlurper();
    
    public CodeAugmentorTask() {
        ObjectFactory objectFactory = getProject().getObjects();
        verbose = objectFactory.property(Boolean.class);
        encoding = objectFactory.property(String.class);
        fileSets = objectFactory.listProperty(ConfigurableFileTree.class);
        augCodeDirectives = objectFactory.listProperty(String.class);
        genCodeStartDirectives = objectFactory.listProperty(String.class);
        genCodeEndDirectives = objectFactory.listProperty(String.class);
        embeddedStringDirectives = objectFactory.listProperty(String.class);
        embeddedJsonDirectives = objectFactory.listProperty(String.class);
        skipCodeStartDirectives = objectFactory.listProperty(String.class);
        skipCodeEndDirectives = objectFactory.listProperty(String.class);
        inlineGenCodeDirectives = objectFactory.listProperty(String.class);
        nestedLevelStartMarkers = objectFactory.listProperty(String.class);
        nestedLevelEndMarkers = objectFactory.listProperty(String.class);

        groovyScriptDir = objectFactory.property(Object.class);
        groovyEntryScriptName = objectFactory.property(String.class);
        
        destDir = objectFactory.property(Object.class);
        codeChangeDetectionDisabled = objectFactory.property(Boolean.class);
        failOnChanges = objectFactory.property(Boolean.class);

        prepFile = objectFactory.property(Object.class);
        augCodeFile = objectFactory.property(Object.class);
        genCodeFile = objectFactory.property(Object.class);
    }

    @TaskAction    
    public void execute() throws GradleException {
        try {
            // Prepare prepFile and aug codes file.
            String resolvedEncoding = encoding.get();
            boolean resolvedVerbose = verbose.get(); 
            List<ConfigurableFileTree> resolvedFileSets = fileSets.get();
            List<String> resolvedGenCodeStartDirectives = genCodeStartDirectives.get();
            List<String> resolvedGenCodeEndDirectives = genCodeEndDirectives.get();
            List<String> resolvedEmbeddedStringDirectives = embeddedStringDirectives.get();
            List<String> resolvedEmbeddedJsonDirectives = embeddedJsonDirectives.get();
            List<String> resolvedSkipCodeStartDirectives = skipCodeStartDirectives.get();
            List<String> resolvedSkipCodeEndDirectives = skipCodeEndDirectives.get();
            List<List<String>> resolvedAugCodeSpecDirectives = Arrays.asList(
                augCodeDirectives.get());
            List<File> resolvedAugCodeFiles = Arrays.asList(getProject().file(augCodeFile));
            File resolvedPrepFile = getProject().file(prepFile);
            List<String> resolvedInlineGenCodeDirectives = inlineGenCodeDirectives.get();
            List<String> resolvedNestedLevelStartMarkers = nestedLevelStartMarkers.get();
            List<String> resolvedNestedLevelEndMarkers = nestedLevelEndMarkers.get();
            PreparationTask.completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedFileSets, resolvedGenCodeStartDirectives,
                resolvedGenCodeEndDirectives, resolvedEmbeddedStringDirectives,
                resolvedEmbeddedJsonDirectives, resolvedSkipCodeStartDirectives,
                resolvedSkipCodeEndDirectives, resolvedAugCodeSpecDirectives,
                resolvedAugCodeFiles, resolvedPrepFile,
                resolvedInlineGenCodeDirectives, 
                resolvedNestedLevelStartMarkers, resolvedNestedLevelEndMarkers);

            // Process aug codes file into gen codes file.
            File resolvedAugCodeFile = getProject().file(augCodeFile);
            File resolvedGenCodeFile = getProject().file(genCodeFile);
            File resolvedGroovyScriptDir = null;
            if (groovyScriptDir.isPresent()) {
                resolvedGroovyScriptDir = getProject().file(groovyScriptDir);
            }
            String resolvedGroovyEntryScriptName = groovyEntryScriptName.get();
            
            executeProcessStage(resolvedVerbose,
                resolvedAugCodeFile, resolvedGenCodeFile,
                resolvedGroovyScriptDir, resolvedGroovyEntryScriptName);

            // Finish off code augmentation with gen codes file.
            File resolvedDestDir = getProject().file(destDir);            
            boolean resolvedCodeChangeDetectionDisabled = codeChangeDetectionDisabled.get();
            boolean resolvedFailOnChanges = failOnChanges.get();
            CompletionTask.completeExecute(this, resolvedVerbose,
                resolvedPrepFile, Arrays.asList(resolvedGenCodeFile), resolvedDestDir,
                resolvedCodeChangeDetectionDisabled, resolvedFailOnChanges);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
        }
    }
    
    private void executeProcessStage(boolean resolvedVerbose,
            File resolvedAugCodeFile, File resolvedGenCodeFile,
            File resolvedGroovyScriptDir, String resolvedGroovyEntryScriptName) throws Exception {
        
        if (resolvedAugCodeFile == null) {
            throw new RuntimeException("unexpected absence of augCodeFile");
        }
        if (resolvedGenCodeFile == null) {
            throw new RuntimeException("unexpected absence of genCodeFile");
        }
        // groovy script dir is required.
        if (resolvedGroovyScriptDir == null) {
            throw new GradleException("groovyScriptDir property is required");
        }
        Logger logger = getLogger();
        ProcessCodeGenericTask genericTask = new ProcessCodeGenericTask();
        genericTask.setLogAppender(TaskUtils.createLogAppender(this, resolvedVerbose));
        genericTask.setInputFile(resolvedAugCodeFile);
        genericTask.setOutputFile(resolvedGenCodeFile);
        genericTask.setJsonParseFunction(s -> JSON_PARSER.parseText(s));

        if (resolvedVerbose) {
            // Print plugin task properties and any extra useful values for user.
            // As much as possible use generic task properties.
            logger.info("Configuration properties:");
            logger.info("\tgroovyScriptDir: " + resolvedGroovyScriptDir);
            logger.info("\tgroovyEntryScriptName: " + resolvedGroovyEntryScriptName);
            logger.info("\tgenericTask.inputFile: " + resolvedAugCodeFile);
            logger.info("\tgenericTask.outputFile: " + resolvedGenCodeFile);
            logger.info("\tgenericTask.logAppender: " + genericTask.getLogAppender());
            logger.info("\tgenericTask.jsonParseFunction: " + genericTask.getJsonParseFunction());
        }

        URL[] scriptEngineRoots = new URL[]{ resolvedGroovyScriptDir.toURI().toURL() };
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setRecompileGroovySource(false);
        scriptEngine.setConfig(cc);
        Binding binding = new Binding();
        binding.setVariable("parentTask", genericTask);
        logger.info("Launching " + resolvedGroovyEntryScriptName + "...");        
        List<Throwable> scriptErrors = new ArrayList<>();
        try {
            scriptEngine.run(resolvedGroovyEntryScriptName, binding);
        }
        catch (Throwable t) {
            scriptErrors.add(t);
        }

        scriptErrors.addAll(genericTask.getAllErrors());

        // fail build if there were errors.
        if (!scriptErrors.isEmpty()) {
            String allExMsg = PluginUtils.stringifyPossibleScriptErrors(
                scriptErrors, true, null, null);
            throw new GradleException(allExMsg);
        }
    }

    @Internal
    public Property<Boolean> getVerbose() {
        return verbose;
    }

    @Internal
    public Property<String> getEncoding() {
        return encoding;
    }

    @Internal
    public ListProperty<ConfigurableFileTree> getFileSets() {
        return fileSets;
    }

    @Internal
    public ListProperty<String> getAugCodeDirectives() {
        return augCodeDirectives;
    }

    @Internal
    public ListProperty<String> getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    @Internal
    public ListProperty<String> getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    @Internal
    public ListProperty<String> getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    @Internal
    public ListProperty<String> getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
    }

    @Internal
    public ListProperty<String> getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    @Internal
    public ListProperty<String> getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    @Internal
    public ListProperty<String> getInlineGenCodeDirectives() {
        return inlineGenCodeDirectives;
    }

    @Internal
    public ListProperty<String> getNestedLevelStartMarkers() {
        return nestedLevelStartMarkers;
    }

    @Internal
    public ListProperty<String> getNestedLevelEndMarkers() {
        return nestedLevelEndMarkers;
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
    public Property<Object> getDestDir() {
        return destDir;
    }

    @Internal
    public Property<Boolean> getCodeChangeDetectionDisabled() {
        return codeChangeDetectionDisabled;
    }

    @Internal
    public Property<Boolean> getFailOnChanges() {
        return failOnChanges;
    }

    @Internal
    public Provider<Object> getPrepFile() {
        return prepFile;
    }

    @Internal
    public Provider<Object> getAugCodeFile() {
        return augCodeFile;
    }

    @Internal
    public Provider<Object> getGenCodeFile() {
        return genCodeFile;
    }
}