package com.aaronicsubstances.code.augmentor.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Source of configuration for prepare and generate plugin tasks.
 */
public class CodeAugmentorPluginExtension {
    private final Property<String> encoding;
    private final ListProperty<ConfigurableFileTree> fileSets;
    private final RegularFileProperty prepFile;
    private final ListProperty<AugCodeDirectiveSpec> augCodeDirectives;
    private final ListProperty<String> genCodeStartDirectives;
    private final ListProperty<String> genCodeEndDirectives;
    private final ListProperty<String> embeddedStringDirectives;
    private final ListProperty<String> embeddedJsonDirectives;
    private final ListProperty<String> enableScanDirectives;
    private final ListProperty<String> disableScanDirectives;
    
    // extra config for generate completion.
    private final ListProperty<RegularFile> generatedCodeFiles;
    private final DirectoryProperty destDir;
    private final RegularFileProperty changeSetInfoFile;
    
    public CodeAugmentorPluginExtension(Project project) {
        ObjectFactory objectFactory = project.getObjects();
        encoding = objectFactory.property(String.class);
        fileSets = objectFactory.listProperty(ConfigurableFileTree.class);
        prepFile = objectFactory.fileProperty();
        augCodeDirectives = objectFactory.listProperty(AugCodeDirectiveSpec.class);
        genCodeStartDirectives = objectFactory.listProperty(String.class);
        genCodeEndDirectives = objectFactory.listProperty(String.class);
        embeddedStringDirectives = objectFactory.listProperty(String.class);
        embeddedJsonDirectives = objectFactory.listProperty(String.class);
        enableScanDirectives = objectFactory.listProperty(String.class);
        disableScanDirectives = objectFactory.listProperty(String.class);

        generatedCodeFiles = objectFactory.listProperty(RegularFile.class);
        destDir = objectFactory.directoryProperty();
        changeSetInfoFile = objectFactory.fileProperty();
    }

    public Property<String> getEncoding() {
        return encoding;
    }

    public ListProperty<ConfigurableFileTree> getFileSets() {
        return fileSets;
    }

    public RegularFileProperty getPrepFile() {
        return prepFile;
    }

    public ListProperty<AugCodeDirectiveSpec> getAugCodeDirectives() {
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

    public ListProperty<String> getEnableScanDirectives() {
        return enableScanDirectives;
    }

    public ListProperty<String> getDisableScanDirectives() {
        return disableScanDirectives;
    }

    public ListProperty<RegularFile> getGeneratedCodeFiles() {
        return generatedCodeFiles;
    }

    public RegularFileProperty getChangeSetInfoFile() {
        return changeSetInfoFile;
    }

    public DirectoryProperty getDestDir() {
        return destDir;
    }
}