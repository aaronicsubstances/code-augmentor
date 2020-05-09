package com.aaronicsubstances.code.augmentor.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import groovy.lang.Closure;

/**
 * Source of configuration for prepare and generate plugin tasks.
 */
public class CodeAugmentorPluginExtension {
    private final Property<String> encoding;
    private final ListProperty<ConfigurableFileTree> fileSets;
    private final Property<Object> prepFile;
    private final ListProperty<AugCodeDirectiveSpec> augCodeSpecs;
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

    // extra config for generate completion.
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Object> destDir;
    private final Property<Boolean> failOnChanges;

    // config for process task.
    private final Property<Object> groovyScriptDir;
    private final Property<String> groovyEntryScriptName;

    private final Property<Boolean> verbose;

    private final Property<Integer> augCodeSpecIndex;
    private final Property<Integer> genCodeFileIndex;

    private final Project project;

    public CodeAugmentorPluginExtension(Project project) {
        this.project = project;
        ObjectFactory objectFactory = project.getObjects();
        encoding = objectFactory.property(String.class);
        fileSets = objectFactory.listProperty(ConfigurableFileTree.class);
        prepFile = objectFactory.property(Object.class);
        augCodeSpecs = objectFactory.listProperty(AugCodeDirectiveSpec.class);
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

        generatedCodeFiles = objectFactory.listProperty(Object.class);
        destDir = objectFactory.property(Object.class);
        failOnChanges = objectFactory.property(Boolean.class);

        groovyScriptDir = objectFactory.property(Object.class);
        groovyEntryScriptName = objectFactory.property(String.class);
        augCodeSpecIndex = objectFactory.property(Integer.class);
        genCodeFileIndex = objectFactory.property(Integer.class);

        verbose = objectFactory.property(Boolean.class);
    }

    public AugCodeDirectiveSpec augCodeSpec(Closure<?> closure) {
        AugCodeDirectiveSpec augCodeDirectiveSpec = new AugCodeDirectiveSpec(project);
        project.configure(augCodeDirectiveSpec, closure);
        return augCodeDirectiveSpec;
    }

    public Property<String> getEncoding() {
        return encoding;
    }

    public ListProperty<ConfigurableFileTree> getFileSets() {
        return fileSets;
    }

    public Property<Object> getPrepFile() {
        return prepFile;
    }

    public ListProperty<AugCodeDirectiveSpec> getAugCodeSpecs() {
        return augCodeSpecs;
    }

    public ListProperty<String> getAugCodeDirectives() {
        return augCodeDirectives;
    }

    public ListProperty<String> getGenCodeStartDirectives() {
        return genCodeStartDirectives;
    }

    public ListProperty<String> getGenCodeEndDirectives() {
        return genCodeEndDirectives;
    }

    public ListProperty<String> getEmbeddedStringDirectives() {
        return embeddedStringDirectives;
    }

    public ListProperty<String> getEmbeddedJsonDirectives() {
        return embeddedJsonDirectives;
    }

    public ListProperty<String> getSkipCodeStartDirectives() {
        return skipCodeStartDirectives;
    }

    public ListProperty<String> getSkipCodeEndDirectives() {
        return skipCodeEndDirectives;
    }

    public ListProperty<String> getInlineGenCodeDirectives() {
        return inlineGenCodeDirectives;
    }

    public ListProperty<String> getNestedLevelStartMarkers() {
        return nestedLevelStartMarkers;
    }

    public ListProperty<String> getNestedLevelEndMarkers() {
        return nestedLevelEndMarkers;
    }

    public ListProperty<Object> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    public Property<Object> getDestDir() {
        return destDir;
    }

    public Property<Boolean> getFailOnChanges() {
        return failOnChanges;
    }

	public Property<Object> getGroovyScriptDir() {
		return groovyScriptDir;
	}

    public Property<String> getGroovyEntryScriptName() {
        return groovyEntryScriptName;
    }

	public Property<Boolean> getVerbose() {
		return verbose;
	}

    public Property<Integer> getAugCodeSpecIndex() {
        return augCodeSpecIndex;
    }

    public Property<Integer> getGenCodeFileIndex() {
        return genCodeFileIndex;
    }
}