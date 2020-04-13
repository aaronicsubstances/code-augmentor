package com.aaronicsubstances.code.augmentor.gradle;

import java.util.Arrays;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class CodeAugmentorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // create extension.
        final String pluginExtName = "code_augmentor";
        CodeAugmentorPluginExtension extension = project.getExtensions().create(
            pluginExtName, CodeAugmentorPluginExtension.class, project);
        
        // set defaults for all required values.
        extension.getEncoding().convention("utf-8");

        DirectoryProperty buildDir = project.getLayout().getBuildDirectory();
        String workingDirPrefix = pluginExtName + "/";
        extension.getPrepFile().convention(buildDir.file(workingDirPrefix + "prepResults.json"));
        extension.getGenCodeStartDirectives().convention(Arrays.asList("//GS:"));
        extension.getGenCodeEndDirectives().convention(Arrays.asList("//:GE"));
        extension.getEmbeddedStringDirectives().convention(Arrays.asList("//ES:"));
        extension.getEmbeddedJsonDirectives().convention(Arrays.asList("//EJS:"));
        
        AugCodeDirectiveSpec defaultAugCodeSpec = new AugCodeDirectiveSpec(project);
        defaultAugCodeSpec.getDestFile().convention(buildDir.file(workingDirPrefix + "augCodes.json"));
        defaultAugCodeSpec.getDirectives().convention(Arrays.asList("//AUG_CODE:"));
        extension.getAugCodeDirectives().convention(Arrays.asList(defaultAugCodeSpec));

        // continue setting defaults for code_aug_generate task.
        extension.getChangeSetInfoFile().convention(buildDir.file(workingDirPrefix + "changeSet.txt"));
        extension.getDestDir().convention(buildDir.dir(workingDirPrefix + "generated"));
        extension.getGeneratedCodeFiles().convention(Arrays.asList(
            buildDir.file(workingDirPrefix + "genCodes.json")));

        // add tasks.
        project.getTasks().register("code_aug_prepare", PreCodeAugmentationTask.class, new Action<PreCodeAugmentationTask>() {

            @Override
            public void execute(PreCodeAugmentationTask prepareTask) {
                prepareTask.getEncoding().set(extension.getEncoding());
                prepareTask.getFileSets().set(extension.getFileSets());
                prepareTask.getPrepFile().set(extension.getPrepFile());
                prepareTask.getAugCodeDirectives().set(extension.getAugCodeDirectives());
                prepareTask.getGenCodeStartDirectives().set(extension.getGenCodeStartDirectives());
                prepareTask.getGenCodeEndDirectives().set(extension.getGenCodeEndDirectives());
                prepareTask.getEmbeddedStringDirectives().set(extension.getEmbeddedStringDirectives());
                prepareTask.getEmbeddedJsonDirectives().set(extension.getEmbeddedJsonDirectives());
                prepareTask.getEnableScanDirectives().set(extension.getEnableScanDirectives());
                prepareTask.getDisableScanDirectives().set(extension.getDisableScanDirectives());
            }
        });
        project.getTasks().register("code_aug_generate", CodeAugmentationTask.class, new Action<CodeAugmentationTask>() {

            @Override
            public void execute(CodeAugmentationTask generateTask) {
                generateTask.getEncoding().set(extension.getEncoding());
                generateTask.getPrepFile().set(extension.getPrepFile());
                generateTask.getGeneratedCodeFiles().set(extension.getGeneratedCodeFiles());
                generateTask.getDestDir().set(extension.getDestDir());
                generateTask.getChangeSetInfoFile().set(extension.getChangeSetInfoFile());
            }
        });
    }
}