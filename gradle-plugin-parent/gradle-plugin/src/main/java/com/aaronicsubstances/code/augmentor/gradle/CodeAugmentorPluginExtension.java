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
    private final ListProperty<String> genCodeStartDirectives;
    private final ListProperty<String> genCodeEndDirectives;
    private final ListProperty<String> embeddedStringDirectives;
    private final ListProperty<String> embeddedJsonDirectives;
    private final ListProperty<String> enableScanDirectives;
    private final ListProperty<String> disableScanDirectives;

    // extra config for generate completion.
    private final ListProperty<Object> generatedCodeFiles;
    private final Property<Object> destDir;
    private final Property<Object> changeSetInfoFile;

    // extra config for process with groovy
    private final Property<Object> scriptsDir;
    private final Property<String> entryScriptName;

    private final Property<Boolean> verbose;

    private final Project project;

    public CodeAugmentorPluginExtension(Project project) {
        this.project = project;
        ObjectFactory objectFactory = project.getObjects();
        encoding = objectFactory.property(String.class);
        fileSets = objectFactory.listProperty(ConfigurableFileTree.class);
        prepFile = objectFactory.property(Object.class);
        augCodeSpecs = objectFactory.listProperty(AugCodeDirectiveSpec.class);
        genCodeStartDirectives = objectFactory.listProperty(String.class);
        genCodeEndDirectives = objectFactory.listProperty(String.class);
        embeddedStringDirectives = objectFactory.listProperty(String.class);
        embeddedJsonDirectives = objectFactory.listProperty(String.class);
        enableScanDirectives = objectFactory.listProperty(String.class);
        disableScanDirectives = objectFactory.listProperty(String.class);

        generatedCodeFiles = objectFactory.listProperty(Object.class);
        destDir = objectFactory.property(Object.class);
        changeSetInfoFile = objectFactory.property(Object.class);

        scriptsDir = objectFactory.property(Object.class);
        entryScriptName = objectFactory.property(String.class);
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

    public ListProperty<String> getEnableScanDirectives() {
        return enableScanDirectives;
    }

    public ListProperty<String> getDisableScanDirectives() {
        return disableScanDirectives;
    }

    public ListProperty<Object> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    public Property<Object> getChangeSetInfoFile() {
        return changeSetInfoFile;
    }

    public Property<Object> getDestDir() {
        return destDir;
    }

	public Property<Object> getScriptsDir() {
		return scriptsDir;
	}

    public Property<String> getEntryScriptName() {
        return entryScriptName;
    }

	public Property<Boolean> getVerbose() {
		return verbose;
	}
}