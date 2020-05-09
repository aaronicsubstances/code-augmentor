package com.aaronicsubstances.code.augmentor.gradle;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

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
    private final Property<Boolean> failOnChanges;

    private final Property<Object> prepFile;
    private final Property<Object> augCodeFile;
    private final Property<Object> genCodeFile;
    
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
            
            ProcessingTask.completeExecute(this, resolvedVerbose, 0, 0, 
                resolvedAugCodeFile, resolvedGenCodeFile,
                resolvedGroovyScriptDir, resolvedGroovyEntryScriptName);

            // Finish off code augmentation with gen codes file.
            File resolvedDestDir = getProject().file(destDir);
            boolean resolvedFailOnChanges = failOnChanges.get();
            CompletionTask.completeExecute(this, resolvedEncoding, resolvedVerbose,
                resolvedPrepFile, Arrays.asList(resolvedGenCodeFile), resolvedDestDir,
                resolvedFailOnChanges);
        }
        catch (GradleException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new GradleException("General plugin error: " + ex, ex);
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